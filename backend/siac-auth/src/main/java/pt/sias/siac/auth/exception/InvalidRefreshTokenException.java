package pt.sias.siac.auth.exception;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("refresh token inválido, expirado ou revogado");
    }
}
