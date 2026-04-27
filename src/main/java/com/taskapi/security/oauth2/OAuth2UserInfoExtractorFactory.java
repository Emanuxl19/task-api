package com.taskapi.security.oauth2;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OAuth2UserInfoExtractorFactory {

    private final List<OAuth2UserInfoExtractor> extractors;

    public OAuth2UserInfoExtractorFactory(List<OAuth2UserInfoExtractor> extractors) {
        this.extractors = extractors;
    }

    public OAuth2UserInfoExtractor getExtractor(String registrationId) {
        return extractors.stream()
            .filter(extractor -> extractor.supports(registrationId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unsupported OAuth2 provider: " + registrationId
            ));
    }
}
