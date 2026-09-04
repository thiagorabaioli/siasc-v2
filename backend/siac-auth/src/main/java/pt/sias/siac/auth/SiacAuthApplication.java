package pt.sias.siac.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SiacAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiacAuthApplication.class, args);
    }
}
