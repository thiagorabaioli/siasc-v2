package pt.sias.siac.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "ambitos")
public class Ambito {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Papel papel;

    @Column(name = "condominio_id")
    private UUID condominioId;

    @Column(name = "fracao_id")
    private UUID fracaoId;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    protected Ambito() {
    }

    public Ambito(Papel papel, UUID condominioId, UUID fracaoId) {
        if (papel.exigeAmbito() && condominioId == null) {
            throw new IllegalArgumentException("papel " + papel + " exige condominio_id");
        }
        if (!papel.exigeAmbito() && (condominioId != null || fracaoId != null)) {
            throw new IllegalArgumentException("papel " + papel + " não aceita condominio_id/fracao_id");
        }
        this.papel = papel;
        this.condominioId = condominioId;
        this.fracaoId = fracaoId;
    }

    public UUID getId() {
        return id;
    }

    public Utilizador getUtilizador() {
        return utilizador;
    }

    void setUtilizador(Utilizador utilizador) {
        this.utilizador = utilizador;
    }

    public Papel getPapel() {
        return papel;
    }

    public UUID getCondominioId() {
        return condominioId;
    }

    public UUID getFracaoId() {
        return fracaoId;
    }
}
