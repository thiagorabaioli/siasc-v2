package pt.sias.siac.auth.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import pt.sias.siac.auth.config.JwtProperties;
import pt.sias.siac.auth.domain.Ambito;
import pt.sias.siac.auth.domain.Utilizador;

@Service
public class JwtService {

    private final RSAKey signingKey;
    private final JwtProperties properties;

    public JwtService(RSAKey signingKey, JwtProperties properties) {
        this.signingKey = signingKey;
        this.properties = properties;
    }

    public String issueAccessToken(Utilizador utilizador) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.accessTokenTtl());

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(utilizador.getId().toString())
                .issuer(properties.issuer())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .jwtID(UUID.randomUUID().toString())
                .claim("email", utilizador.getEmail())
                .claim("nome", utilizador.getNome())
                .claim("deveTrocarPassword", utilizador.isDeveTrocarPassword())
                .claim("ambitos", toClaim(utilizador.getAmbitos()))
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(signingKey.getKeyID())
                .type(com.nimbusds.jose.JOSEObjectType.JWT)
                .build();

        SignedJWT jwt = new SignedJWT(header, claims);
        try {
            jwt.sign(new RSASSASigner(signingKey));
        } catch (JOSEException e) {
            throw new IllegalStateException("falha a assinar JWT", e);
        }
        return jwt.serialize();
    }

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    /**
     * Decodifica claims sem validar assinatura — só para uso interno (ex.:
     * logs). A validação de tokens de acesso é feita pelos resource servers
     * via JWKS, não pelo siac-auth.
     */
    public JWTClaimsSet parseUnverified(String token) throws ParseException {
        return SignedJWT.parse(token).getJWTClaimsSet();
    }

    private List<Map<String, Object>> toClaim(List<Ambito> ambitos) {
        return ambitos.stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("papel", a.getPapel().name());
                    m.put("condominioId", a.getCondominioId() != null ? a.getCondominioId().toString() : null);
                    m.put("fracaoId", a.getFracaoId() != null ? a.getFracaoId().toString() : null);
                    return m;
                })
                .toList();
    }
}
