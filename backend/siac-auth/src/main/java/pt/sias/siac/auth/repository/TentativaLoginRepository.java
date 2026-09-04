package pt.sias.siac.auth.repository;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.sias.siac.auth.domain.TentativaLogin;

public interface TentativaLoginRepository extends JpaRepository<TentativaLogin, UUID> {

    long countByEmailIgnoreCaseAndSucessoFalseAndCriadoEmAfter(String email, Instant desde);

    TentativaLogin findFirstByEmailIgnoreCaseAndSucessoFalseAndCriadoEmAfterOrderByCriadoEmDesc(
            String email, Instant desde);
}
