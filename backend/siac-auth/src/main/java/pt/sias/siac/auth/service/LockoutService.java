package pt.sias.siac.auth.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pt.sias.siac.auth.config.LockoutProperties;
import pt.sias.siac.auth.domain.TentativaLogin;
import pt.sias.siac.auth.exception.AccountLockedException;
import pt.sias.siac.auth.repository.TentativaLoginRepository;

/**
 * RF-AUTH-06 — bloqueio progressivo. Conta falhas consecutivas desde o
 * último login bem-sucedido (limitado a uma janela de lookback para não
 * fazer scans ilimitados); a partir de {@code maxAttempts} cada falha
 * adicional duplica a duração do bloqueio, com um teto.
 */
@Service
public class LockoutService {

    private static final int LOOKBACK_HOURS = 24;

    private final TentativaLoginRepository repository;
    private final LockoutProperties properties;

    public LockoutService(TentativaLoginRepository repository, LockoutProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public void assertNaoBloqueado(String email) {
        Instant lookback = Instant.now().minus(LOOKBACK_HOURS, ChronoUnit.HOURS);
        long falhas = repository.countByEmailIgnoreCaseAndSucessoFalseAndCriadoEmAfter(email, lookback);
        if (falhas < properties.maxAttempts()) {
            return;
        }

        TentativaLogin ultimaFalha =
                repository.findFirstByEmailIgnoreCaseAndSucessoFalseAndCriadoEmAfterOrderByCriadoEmDesc(
                        email, lookback);
        if (ultimaFalha == null) {
            return;
        }

        int nivel = (int) (falhas - properties.maxAttempts() + 1);
        long minutosBloqueio = Math.min(
                properties.baseMinutes() * (1L << Math.min(nivel - 1, 20)),
                properties.capMinutes());
        Instant bloqueadoAte = ultimaFalha.getCriadoEm().plus(minutosBloqueio, ChronoUnit.MINUTES);

        Instant agora = Instant.now();
        if (agora.isBefore(bloqueadoAte)) {
            long retryAfter = ChronoUnit.SECONDS.between(agora, bloqueadoAte);
            throw new AccountLockedException(retryAfter);
        }
    }

    /**
     * REQUIRES_NEW de propósito: chamada a partir de AuthService.login(),
     * que lança exceção (e faz rollback) quando a tentativa falha — o
     * registo da falha tem de sobreviver a esse rollback para o lockout
     * funcionar.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registarTentativa(String email, boolean sucesso, String ip) {
        repository.save(new TentativaLogin(email, sucesso, ip));
    }
}
