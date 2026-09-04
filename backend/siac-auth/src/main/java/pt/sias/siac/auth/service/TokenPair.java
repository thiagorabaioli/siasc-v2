package pt.sias.siac.auth.service;

public record TokenPair(
        String accessToken,
        long accessTokenExpiresIn,
        String refreshToken,
        long refreshTokenExpiresIn,
        boolean deveTrocarPassword) {
}
