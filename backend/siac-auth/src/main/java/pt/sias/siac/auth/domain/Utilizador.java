package pt.sias.siac.auth.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "utilizadores")
public class Utilizador {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "deve_trocar_password", nullable = false)
    private boolean deveTrocarPassword = false;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    @OneToMany(mappedBy = "utilizador", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Ambito> ambitos = new ArrayList<>();

    protected Utilizador() {
    }

    public Utilizador(String email, String passwordHash, String nome) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nome = nome;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNome() {
        return nome;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public boolean isDeveTrocarPassword() {
        return deveTrocarPassword;
    }

    public void setDeveTrocarPassword(boolean deveTrocarPassword) {
        this.deveTrocarPassword = deveTrocarPassword;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public List<Ambito> getAmbitos() {
        return ambitos;
    }

    public void adicionarAmbito(Ambito ambito) {
        ambitos.add(ambito);
        ambito.setUtilizador(this);
    }
}
