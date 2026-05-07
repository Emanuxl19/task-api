package com.taskapi.security.oauth2;

import com.taskapi.entity.AuthProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GoogleUserInfoExtractor implements OAuth2UserInfoExtractor {

    @Override
    public boolean supports(String registrationId) {
        return "google".equalsIgnoreCase(registrationId);
    }

    @Override
    public String nameAttributeKey() {
        return "sub";
    }

    @Override
    public OAuth2UserInfo extract(Map<String, Object> attributes) {
        String providerId = stringValue(attributes.get("sub"));
        String email = stringValue(attributes.get("email"));
        String name = firstNonBlank(stringValue(attributes.get("name")), email);

        return new OAuth2UserInfo(providerId, email, name, AuthProvider.GOOGLE);
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }
}
