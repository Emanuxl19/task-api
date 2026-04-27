package com.taskapi.security.oauth2;

import com.taskapi.entity.AuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OAuth2UserInfoExtractorFactory")
class OAuth2UserInfoExtractorFactoryTest {

    private final GoogleUserInfoExtractor googleExtractor = new GoogleUserInfoExtractor();
    private final GitHubUserInfoExtractor gitHubExtractor = new GitHubUserInfoExtractor();
    private final OAuth2UserInfoExtractorFactory factory =
        new OAuth2UserInfoExtractorFactory(List.of(googleExtractor, gitHubExtractor));

    @Test
    @DisplayName("returns google extractor and maps google attributes")
    void shouldReturnGoogleExtractor() {
        var extractor = factory.getExtractor("google");
        var userInfo = extractor.extract(Map.of(
            "sub", "google-123",
            "email", "ana@email.com",
            "name", "Ana"
        ));

        assertThat(extractor).isSameAs(googleExtractor);
        assertThat(userInfo.providerId()).isEqualTo("google-123");
        assertThat(userInfo.email()).isEqualTo("ana@email.com");
        assertThat(userInfo.name()).isEqualTo("Ana");
        assertThat(userInfo.authProvider()).isEqualTo(AuthProvider.GOOGLE);
    }

    @Test
    @DisplayName("returns github extractor and falls back to login when name is absent")
    void shouldReturnGithubExtractor() {
        var extractor = factory.getExtractor("github");
        var userInfo = extractor.extract(Map.of(
            "id", 42L,
            "email", "bruno@email.com",
            "login", "brunodev"
        ));

        assertThat(extractor).isSameAs(gitHubExtractor);
        assertThat(userInfo.providerId()).isEqualTo("42");
        assertThat(userInfo.email()).isEqualTo("bruno@email.com");
        assertThat(userInfo.name()).isEqualTo("brunodev");
        assertThat(userInfo.authProvider()).isEqualTo(AuthProvider.GITHUB);
    }

    @Test
    @DisplayName("throws when provider is unsupported")
    void shouldThrowWhenProviderIsUnsupported() {
        assertThatThrownBy(() -> factory.getExtractor("facebook"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("facebook");
    }
}
