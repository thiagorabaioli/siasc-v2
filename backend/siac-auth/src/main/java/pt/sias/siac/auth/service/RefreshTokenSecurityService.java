package pt.sias.siac.auth.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pt.sias.siac.auth.domain.RefreshToken;
import pt.sias.siac.auth.repository.RefreshTokenRepository;

/**
 * Isolado de AuthService de propósito: REQUIRES_NEW só funciona através de
 * um bean diferente (proxy do Spring), nunca numa chamada interna dentro da
 * mesma classe. Usado quando se deteta reutilização de um refresh token já
 * rodado — a revogação de tudo o resto tem de sobreviver ao rollback do
 * InvalidRefreshTokenException lançado a seguir em AuthService.refresh().
 */
@Service
public class RefreshTokenSecurityService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenSecurityService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revogarTodosAtivos(UUID utilizadorId) {
        List<RefreshToken> ativos = refreshTokenRepository.findByUtilizadorIdAndRevogadoEmIsNull(utilizadorId);
        ativos.forEach(RefreshToken::revogar);
        refreshTokenRepository.saveAll(ativos);
    }
}
