package ru.masterskaya.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
@ConfigurationProperties(prefix = "logging.masking")
@Getter
@Setter
@ToString
public class MaskingProperties {
    private boolean enabled = true;
    private Set<String> sensitiveFields = new HashSet<>();
    private Map<String, String> patterns = new HashMap<>();

    @PostConstruct
    void init() {
        // Дефолтные значения
        if (sensitiveFields.isEmpty()) {
            sensitiveFields.addAll(Arrays.asList(
                    "password", "pass", "pwd", "secret", "token", "jwt"
            ));
        }
    }
}
