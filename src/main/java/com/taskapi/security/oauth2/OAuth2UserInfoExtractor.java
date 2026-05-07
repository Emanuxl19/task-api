package com.taskapi.security.oauth2;

import com.taskapi.entity.AuthProvider;

import java.util.Map;

public interface OAuth2UserInfoExtractor {

    boolean supports(String registrationId);

    String nameAttributeKey();

    OAuth2UserInfo extract(Map<String, Object> attributes);

    record OAuth2UserInfo(
        String providerId,
        String email,
        String name,
        AuthProvider authProvider
    ) {}
}
