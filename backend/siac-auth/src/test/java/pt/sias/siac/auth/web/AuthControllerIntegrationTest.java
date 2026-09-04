package pt.sias.siac.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pt.sias.siac.auth.domain.Ambito;
import pt.sias.siac.auth.domain.Papel;
import pt.sias.siac.auth.domain.Utilizador;
import pt.sias.siac.auth.repository.UtilizadorRepository;

/**
 * Matriz de segurança de docs/testes.md §2, adaptada ao siac-auth: aqui não
 * há âmbito de condomínio para testar (isso é siac-core, Fase 2) — em vez
 * disso cobre-se o que docs/testes.md pede especificamente a este serviço:
 * lockout progressivo, refresh token revogado/reutilizado → 401, e hash de
 * password nunca presente em respostas.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    private static final String ADMIN_SEED_EMAIL = "admin-seed-it@siasc.pt";
    private static final String ADMIN_SEED_PASSWORD = "TrocaEstaPassword123!";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void lockoutEAdminSeed(DynamicPropertyRegistry registry) {
        registry.add("siac.auth.lockout.max-attempts", () -> "3");
        registry.add("siac.auth.lockout.base-minutes", () -> "60");
        registry.add("siac.auth.admin-seed.email", () -> ADMIN_SEED_EMAIL);
        registry.add("siac.auth.admin-seed.password", () -> ADMIN_SEED_PASSWORD);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UtilizadorRepository utilizadorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RSAKey signingKey;

    private String novoEmail() {
        return "user-" + UUID.randomUUID() + "@teste.siasc.pt";
    }

    private static final String PASSWORD = "PasswordValida123!";

    private Utilizador criarUtilizador(String email) {
        Utilizador u = new Utilizador(email, passwordEncoder.encode(PASSWORD), "Utilizador de teste");
        u.adicionarAmbito(new Ambito(Papel.GESTOR_CONDOMINIO, UUID.randomUUID(), null));
        return utilizadorRepository.save(u);
    }

    private String loginJson(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new java.util.HashMap<>() {{
            put("email", email);
            put("password", password);
        }});
    }

    @Test
    void login_comCredenciaisValidas_devolve200ETokens() throws Exception {
        String email = novoEmail();
        criarUtilizador(email);

        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.deveTrocarPassword").value(false))
                .andReturn();

        Cookie refreshCookie = result.getResponse().getCookie("siac_refresh");
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.isHttpOnly()).isTrue();
        assertThat(refreshCookie.getSecure()).isTrue();
        assertThat(refreshCookie.getPath()).isEqualTo("/api/auth");
    }

    @Test
    void login_comPasswordErrada_devolve401() throws Exception {
        String email = novoEmail();
        criarUtilizador(email);

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "password-errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_credentials"));
    }

    @Test
    void login_comEmailInexistente_devolve401ComMensagemGenerica() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(novoEmail(), "qualquer-coisa")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_credentials"));
    }

    @Test
    void login_aposNFalhas_devolve429ComRetryAfter() throws Exception {
        String email = novoEmail();
        criarUtilizador(email);

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(email, "errada")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, PASSWORD)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("account_locked"))
                .andExpect(result -> assertThat(result.getResponse().getHeader("Retry-After")).isNotNull());
    }

    @Test
    void refresh_comCookieValido_rodaTokenEInvalidaOAntigo() throws Exception {
        String email = novoEmail();
        criarUtilizador(email);

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie primeiroRefresh = loginResult.getResponse().getCookie("siac_refresh");
        assertThat(primeiroRefresh).isNotNull();

        MvcResult refreshResult = mockMvc.perform(post("/refresh").cookie(primeiroRefresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        Cookie segundoRefresh = refreshResult.getResponse().getCookie("siac_refresh");
        assertThat(segundoRefresh).isNotNull();
        assertThat(segundoRefresh.getValue()).isNotEqualTo(primeiroRefresh.getValue());

        // reutilizar o primeiro refresh (já rodado) tem de falhar
        mockMvc.perform(post("/refresh").cookie(primeiroRefresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_refresh_token"));

        // reutilização também revoga o segundo (já emitido) — deteção de furto
        mockMvc.perform(post("/refresh").cookie(segundoRefresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_semCookie_devolve401() throws Exception {
        mockMvc.perform(post("/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_refresh_token"));
    }

    @Test
    void refresh_tokenExpirado_devolve401() throws Exception {
        String email = novoEmail();
        criarUtilizador(email);

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refresh = loginResult.getResponse().getCookie("siac_refresh");
        assertThat(refresh).isNotNull();

        String hash = sha256(refresh.getValue());
        int updated = jdbcTemplate.update(
                "UPDATE siac_auth.refresh_tokens SET expira_em = ? WHERE token_hash = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(3600)), hash);
        assertThat(updated).isEqualTo(1);

        mockMvc.perform(post("/refresh").cookie(refresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_refresh_token"));
    }

    @Test
    void logout_revogaRefreshToken_refreshSubsequenteDevolve401() throws Exception {
        String email = novoEmail();
        criarUtilizador(email);

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refresh = loginResult.getResponse().getCookie("siac_refresh");
        assertThat(refresh).isNotNull();

        mockMvc.perform(post("/logout").cookie(refresh))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/refresh").cookie(refresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_refresh_token"));
    }

    @Test
    void jwks_semAutenticacao_devolveChavePublica() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].kid").value(signingKey.getKeyID()))
                .andExpect(jsonPath("$.keys[0].d").doesNotExist()); // nunca a chave privada
    }

    @Test
    void accessToken_assinaturaValidaContraJwksPublicado() throws Exception {
        String email = novoEmail();
        criarUtilizador(email);

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        MvcResult jwksResult = mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andReturn();
        JWKSet jwkSet = JWKSet.parse(jwksResult.getResponse().getContentAsString());
        RSAKey publicKey = (RSAKey) jwkSet.getKeys().get(0);

        SignedJWT jwt = SignedJWT.parse(accessToken);
        assertThat(jwt.verify(new RSASSAVerifier(publicKey))).isTrue();
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("siac-auth");
        assertThat(jwt.getJWTClaimsSet().getStringClaim("email")).isEqualTo(email);
        assertThat(jwt.getJWTClaimsSet().getClaim("ambitos")).isNotNull();
    }

    @Test
    void respostaDeLogin_nuncaContemPasswordHash() throws Exception {
        String email = novoEmail();
        Utilizador u = criarUtilizador(email);

        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain(u.getPasswordHash());
        assertThat(body.toLowerCase()).doesNotContain("passwordhash");
    }

    @Test
    void seedAdminSiac_consegueLoginEDeveTrocarPassword() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(ADMIN_SEED_EMAIL, ADMIN_SEED_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deveTrocarPassword").value(true));
    }

    private String sha256(String raw) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        return java.util.HexFormat.of().formatHex(digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
