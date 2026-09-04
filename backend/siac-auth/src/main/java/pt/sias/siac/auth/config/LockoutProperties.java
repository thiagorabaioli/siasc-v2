package pt.sias.siac.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "siac.auth.lockout")
public record LockoutProperties(
        Integer maxAttempts,
        Integer baseMinutes,
        Integer capMinutes) {

    public LockoutProperties {
        if (maxAttempts == null) {
            maxAttempts = 5;
        }
        if (baseMinutes == null) {
            baseMinutes = 1;
        }
        if (capMinutes == null) {
            capMinutes = 60;
        }
    }
}
