package com.taskapi.security.oauth2;

import com.taskapi.entity.AuthProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GitHubUserInfoExtractor implements OAuth2UserInfoExtractor {

    @Override
    public boolean supports(String registrationId) {
        return "github".equalsIgnoreCase(registrationId);
    }

    @Override
    public String nameAttributeKey() {
        return "id";
    }

    @Override
    public OAuth2UserInfo extract(Map<String, Object> attributes) {
        String providerId = stringValue(attributes.get("id"));
        String email = stringValue(attributes.get("email"));
        String login = stringValue(attributes.get("login"));
        String name = firstNonBlank(stringValue(attributes.get("name")), login, email);

        return new OAuth2UserInfo(providerId, email, name, AuthProvider.GITHUB);
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }
}
