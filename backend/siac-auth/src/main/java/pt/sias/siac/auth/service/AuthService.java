package pt.sias.siac.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sias.siac.auth.config.JwtProperties;
import pt.sias.siac.auth.domain.RefreshToken;
import pt.sias.siac.auth.domain.Utilizador;
import pt.sias.siac.auth.exception.InvalidCredentialsException;
import pt.sias.siac.auth.exception.InvalidRefreshTokenException;
import pt.sias.siac.auth.repository.RefreshTokenRepository;
import pt.sias.siac.auth.repository.UtilizadorRepository;

@Service
public class AuthService {

    // Hash de uma password aleatória, só para gastar tempo de BCrypt quando o
    // utilizador não existe — evita que o tempo de resposta denuncie contas
    // válidas (user enumeration por timing).
    private static final String DUMMY_HASH =
            new BCryptPasswordEncoder().encode("dummy-nao-e-uma-conta-real");

    private final UtilizadorRepository utilizadorRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LockoutService lockoutService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final RefreshTokenSecurityService refreshTokenSecurityService;

    public AuthService(
            UtilizadorRepository utilizadorRepository,
            RefreshTokenRepository refreshTokenRepository,
            LockoutService lockoutService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            JwtProperties jwtProperties,
            RefreshTokenSecurityService refreshTokenSecurityService) {
        this.utilizadorRepository = utilizadorRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.lockoutService = lockoutService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
        this.refreshTokenSecurityService = refreshTokenSecurityService;
    }

    @Transactional
    public TokenPair login(String email, String rawPassword, String ip) {
        String normalizedEmail = normalizar(email);
        lockoutService.assertNaoBloqueado(normalizedEmail);

        Optional<Utilizador> maybeUser = utilizadorRepository.findByEmailIgnoreCase(normalizedEmail);
        boolean senhaCorreta;
        if (maybeUser.isEmpty() || !maybeUser.get().isAtivo()) {
            passwordEncoder.matches(rawPassword, DUMMY_HASH);
            senhaCorreta = false;
        } else {
            senhaCorreta = passwordEncoder.matches(rawPassword, maybeUser.get().getPasswordHash());
        }

        if (!senhaCorreta) {
            lockoutService.registarTentativa(normalizedEmail, false, ip);
            throw new InvalidCredentialsException();
        }

        lockoutService.registarTentativa(normalizedEmail, true, ip);
        return emitirTokens(maybeUser.get());
    }

    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (token.getRevogadoEm() != null) {
            // Reutilização de um token já rodado — indício de furto: revoga
            // tudo o que estiver ativo para este utilizador.
            refreshTokenSecurityService.revogarTodosAtivos(token.getUtilizador().getId());
            throw new InvalidRefreshTokenException();
        }
        if (!token.isValido(Instant.now())) {
            throw new InvalidRefreshTokenException();
        }

        token.revogar();
        refreshTokenRepository.save(token);

        Utilizador utilizador = utilizadorRepository.findById(token.getUtilizador().getId())
                .orElseThrow(InvalidRefreshTokenException::new);
        return emitirTokens(utilizador);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .filter(t -> t.getRevogadoEm() == null)
                .ifPresent(t -> {
                    t.revogar();
                    refreshTokenRepository.save(t);
                });
    }

    private TokenPair emitirTokens(Utilizador utilizador) {
        String accessToken = jwtService.issueAccessToken(utilizador);

        String rawRefresh = gerarTokenOpaco();
        Instant expiraEm = Instant.now().plus(jwtProperties.refreshTokenTtl());
        refreshTokenRepository.save(new RefreshToken(utilizador, hash(rawRefresh), expiraEm));

        return new TokenPair(
                accessToken,
                jwtService.accessTokenTtlSeconds(),
                rawRefresh,
                jwtProperties.refreshTokenTtl().toSeconds(),
                utilizador.isDeveTrocarPassword());
    }

    private String gerarTokenOpaco() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
