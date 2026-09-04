package pt.sias.siac.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * siac-auth não guarda sessão (stateless): login/refresh/logout e o JWKS são
 * públicos, tudo o resto fica fechado — este serviço não tem endpoints de
 * dados (gestão de utilizadores/âmbitos fica para siac-core na Fase 2, ver
 * docs/tarefas.md). CSRF fica desligado porque não há autenticação por
 * cookie de sessão nem formulários — o único cookie emitido (refresh token)
 * é SameSite=Strict e só é lido pelos próprios endpoints de auth, o que já
 * neutraliza CSRF cross-site em browsers atuais.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                // logout próprio (AuthController) — o LogoutFilter por omissão
                // intercetava POST /logout antes do controller e respondia
                // 302 para /login?logout.
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(new AntPathRequestMatcher("/login", "POST")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/refresh", "POST")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/logout", "POST")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/.well-known/jwks.json", "GET")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/actuator/health", "GET")).permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
