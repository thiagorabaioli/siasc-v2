package pt.sias.siac.auth.service;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.sias.siac.auth.config.AdminSeedProperties;
import pt.sias.siac.auth.domain.Ambito;
import pt.sias.siac.auth.domain.Papel;
import pt.sias.siac.auth.domain.Utilizador;
import pt.sias.siac.auth.repository.UtilizadorRepository;

/**
 * Seed idempotente do utilizador ADMIN_SIAC inicial (docs/tarefas.md, Fase
 * 1). Só corre se SIAC_ADMIN_INITIAL_PASSWORD estiver definida; nunca
 * sobrescreve um utilizador já existente com o mesmo email. Força troca de
 * password no primeiro login.
 */
@Component
public class AdminSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeedRunner.class);
    private static final String DEFAULT_EMAIL = "admin@siasc.pt";

    private final UtilizadorRepository utilizadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties properties;

    public AdminSeedRunner(
            UtilizadorRepository utilizadorRepository,
            PasswordEncoder passwordEncoder,
            AdminSeedProperties properties) {
        this.utilizadorRepository = utilizadorRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (properties.password() == null || properties.password().isBlank()) {
            log.warn("SIAC_ADMIN_INITIAL_PASSWORD não definida — seed do utilizador ADMIN_SIAC ignorado");
            return;
        }

        String email = (properties.email() == null || properties.email().isBlank())
                ? DEFAULT_EMAIL
                : properties.email().trim().toLowerCase(Locale.ROOT);

        if (utilizadorRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        Utilizador admin = new Utilizador(email, passwordEncoder.encode(properties.password()), "Administrador SIASC");
        admin.setDeveTrocarPassword(true);
        admin.adicionarAmbito(new Ambito(Papel.ADMIN_SIAC, null, null));
        utilizadorRepository.save(admin);
        log.info("Utilizador ADMIN_SIAC inicial criado ({})", email);
    }
}
