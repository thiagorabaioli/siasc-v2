package pt.sias.siac.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "tentativas_login")
public class TentativaLogin {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private boolean sucesso;

    @Column
    private String ip;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    protected TentativaLogin() {
    }

    public TentativaLogin(String email, boolean sucesso, String ip) {
        this.email = email;
        this.sucesso = sucesso;
        this.ip = ip;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public boolean isSucesso() {
        return sucesso;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
