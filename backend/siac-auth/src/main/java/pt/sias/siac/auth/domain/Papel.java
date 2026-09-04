package pt.sias.siac.auth.domain;

public enum Papel {
    SUPER_ADMIN_SIAC,
    ADMIN_SIAC,
    GESTOR_CONDOMINIO,
    REPRESENTANTE_CONDOMINIO,
    TECNICO_FORNECEDOR,
    CONDOMINO;

    public boolean exigeAmbito() {
        return this != SUPER_ADMIN_SIAC && this != ADMIN_SIAC;
    }
}
