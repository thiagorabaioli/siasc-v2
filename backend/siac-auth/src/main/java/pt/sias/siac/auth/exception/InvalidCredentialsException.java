package pt.sias.siac.auth.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("email ou password inválidos");
    }
}
