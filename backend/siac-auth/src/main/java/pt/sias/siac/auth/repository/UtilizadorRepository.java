package pt.sias.siac.auth.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.sias.siac.auth.domain.Utilizador;

public interface UtilizadorRepository extends JpaRepository<Utilizador, UUID> {
    Optional<Utilizador> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
