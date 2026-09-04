package pt.sias.siac.auth.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pt.sias.siac.auth.dto.LoginRequest;
import pt.sias.siac.auth.dto.TokenResponse;
import pt.sias.siac.auth.service.AuthService;
import pt.sias.siac.auth.service.TokenPair;

/**
 * RF-AUTH-01 — o refresh token nunca chega ao JS: viaja num cookie
 * HttpOnly+Secure+SameSite=Strict, com Path fixo ao prefixo público
 * `/api/auth` (nginx mantém esse prefixo do lado do browser mesmo que o
 * proxy_pass o remova antes de chegar aqui — ver frontend/nginx.conf). O
 * access token vai só no corpo da resposta, para o frontend guardar em
 * memória (nunca localStorage, ver CLAUDE.md regra 1 / docs/arquitetura.md §7).
 */
@RestController
public class AuthController {

    private static final String COOKIE_NAME = "siac_refresh";
    private static final String COOKIE_PATH = "/api/auth";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        TokenPair pair = authService.login(request.email(), request.password(), clientIp(servletRequest));
        return withRefreshCookie(pair);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@CookieValue(name = COOKIE_NAME, required = false) String refreshToken) {
        TokenPair pair = authService.refresh(refreshToken);
        return withRefreshCookie(pair);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        ResponseCookie expired = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, expired.toString()).build();
    }

    private ResponseEntity<TokenResponse> withRefreshCookie(TokenPair pair) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, pair.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(pair.refreshTokenExpiresIn())
                .build();
        TokenResponse body = TokenResponse.bearer(pair.accessToken(), pair.accessTokenExpiresIn(), pair.deveTrocarPassword());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(body);
    }

    private String clientIp(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        return (realIp != null && !realIp.isBlank()) ? realIp : request.getRemoteAddr();
    }
}
