package ru.masterskaya.annotation.mask;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;


@Component
public class MaskingRegistry {
    private final Map<Mask.MaskType, MaskingStrategy> strategies = new EnumMap<>(Mask.MaskType.class);

    @PostConstruct
    void init() {
        strategies.put(Mask.MaskType.FULL, value -> "******");
        strategies.put(Mask.MaskType.PARTIAL, value -> {
            if (value == null || value.length() <= 6) return "******";
            return value.substring(0, 3) + "***" + value.substring(value.length() - 3);
        });
        strategies.put(Mask.MaskType.EMAIL, this::maskEmail);
        strategies.put(Mask.MaskType.JWT, this::maskJwt);
    }

    public String mask(String value, Mask.MaskType type) {
        return value == null ? null : strategies.get(type).mask(value);
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "******";
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private String maskJwt(String token) {
        String[] parts = token.split("\\.");
        if (parts.length == 3) {
            return parts[0] + ".***.***";
        }
        return "******";
    }
}
