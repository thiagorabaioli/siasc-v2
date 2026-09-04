package pt.sias.siac.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "siac.auth.admin-seed")
public record AdminSeedProperties(String email, String password) {
}
