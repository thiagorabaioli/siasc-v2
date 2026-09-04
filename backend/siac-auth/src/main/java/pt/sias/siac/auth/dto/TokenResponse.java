package pt.sias.siac.auth.dto;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        boolean deveTrocarPassword) {

    public static TokenResponse bearer(String accessToken, long expiresInSeconds, boolean deveTrocarPassword) {
        return new TokenResponse(accessToken, "Bearer", expiresInSeconds, deveTrocarPassword);
    }
}
