package pt.sias.siac.auth.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Par de chaves RSA gerado em memória no arranque do processo. Um reinício
 * troca a chave — tokens de acesso emitidos antes deixam de validar, o que é
 * aceitável dado o TTL curto (ver {@link JwtProperties}). Refresh tokens são
 * opacos e guardados em BD, não são afetados. Persistir a chave entre
 * reinícios fica para uma decisão de produção posterior.
 */
@Configuration
public class JwtKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyConfig.class);

    @Bean
    public RSAKey siacAuthSigningKey() throws Exception {
        String kid = "siac-auth-" + UUID.randomUUID();
        RSAKey key = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID(kid)
                .generate();
        log.info("Chave RSA de assinatura JWT gerada em memória (kid={})", kid);
        return key;
    }
}
