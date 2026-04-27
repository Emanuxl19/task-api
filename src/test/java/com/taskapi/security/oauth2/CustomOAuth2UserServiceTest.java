package com.taskapi.security.oauth2;

import com.taskapi.entity.AuthProvider;
import com.taskapi.entity.Role;
import com.taskapi.entity.User;
import com.taskapi.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomOAuth2UserService")
class CustomOAuth2UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    private final OAuth2UserInfoExtractorFactory extractorFactory =
        new OAuth2UserInfoExtractorFactory(List.of(
            new GoogleUserInfoExtractor(),
            new GitHubUserInfoExtractor()
        ));

    @Nested
    @DisplayName("loadUser()")
    class LoadUser {

        @Test
        @DisplayName("creates a new user when provider user is not found")
        void shouldCreateNewUser() {
            var service = new CustomOAuth2UserService(extractorFactory, userRepository, delegate);
            var request = oauth2UserRequest("google");
            var oauthUser = googleUser("google-1", "ana@email.com", "Ana");
            var savedUser = makeUser(1L, "Ana", "ana@email.com", AuthProvider.GOOGLE, "google-1");

            when(delegate.loadUser(request)).thenReturn(oauthUser);
            when(userRepository.findByProviderIdAndAuthProvider("google-1", AuthProvider.GOOGLE))
                .thenReturn(Optional.empty());
            when(userRepository.findByEmail("ana@email.com")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            var result = service.loadUser(request);

            assertThat((Long) result.getAttribute("appUserId")).isEqualTo(1L);
            assertThat(result.getAuthorities())
                .extracting(Object::toString)
                .contains("ROLE_USER");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("links an existing local user by email")
        void shouldLinkExistingLocalUser() {
            var service = new CustomOAuth2UserService(extractorFactory, userRepository, delegate);
            var request = oauth2UserRequest("github");
            var oauthUser = githubUser(42L, "bruno@email.com", "bruno-dev", null);
            var localUser = makeUser(2L, "Bruno", "bruno@email.com", AuthProvider.LOCAL, null);

            when(delegate.loadUser(request)).thenReturn(oauthUser);
            when(userRepository.findByProviderIdAndAuthProvider("42", AuthProvider.GITHUB))
                .thenReturn(Optional.empty());
            when(userRepository.findByEmail("bruno@email.com")).thenReturn(Optional.of(localUser));
            when(userRepository.save(localUser)).thenReturn(localUser);

            var result = service.loadUser(request);

            assertThat(localUser.getAuthProvider()).isEqualTo(AuthProvider.GITHUB);
            assertThat(localUser.getProviderId()).isEqualTo("42");
            assertThat((Long) result.getAttribute("appUserId")).isEqualTo(2L);
        }

        @Test
        @DisplayName("throws when account is already linked to another provider")
        void shouldThrowWhenAccountIsLinkedToAnotherProvider() {
            var service = new CustomOAuth2UserService(extractorFactory, userRepository, delegate);
            var request = oauth2UserRequest("github");
            var oauthUser = githubUser(42L, "ana@email.com", "ana-dev", "Ana");
            var googleUser = makeUser(3L, "Ana", "ana@email.com", AuthProvider.GOOGLE, "google-1");

            when(delegate.loadUser(request)).thenReturn(oauthUser);
            when(userRepository.findByProviderIdAndAuthProvider("42", AuthProvider.GITHUB))
                .thenReturn(Optional.empty());
            when(userRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(googleUser));

            assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("already linked");
        }

        @Test
        @DisplayName("throws when provider does not return an email")
        void shouldThrowWhenEmailIsMissing() {
            var service = new CustomOAuth2UserService(extractorFactory, userRepository, delegate);
            var request = oauth2UserRequest("github");

            when(delegate.loadUser(request)).thenReturn(githubUser(42L, null, "bruno-dev", "Bruno"));

            assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("Email");
        }
    }

    private OAuth2UserRequest oauth2UserRequest(String registrationId) {
        var clientRegistration = ClientRegistration.withRegistrationId(registrationId)
            .clientId("client-id")
            .clientSecret("client-secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .authorizationUri("https://example.com/oauth/authorize")
            .tokenUri("https://example.com/oauth/token")
            .userInfoUri("https://example.com/userinfo")
            .userNameAttributeName("id")
            .clientName(registrationId)
            .build();

        var accessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "access-token",
            Instant.now(),
            Instant.now().plusSeconds(3600)
        );

        return new OAuth2UserRequest(clientRegistration, accessToken);
    }

    private OAuth2User googleUser(String sub, String email, String name) {
        return new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("SCOPE_email")),
            java.util.Map.of("sub", sub, "email", email, "name", name),
            "sub"
        );
    }

    private OAuth2User githubUser(Long id, String email, String login, String name) {
        var attributes = new HashMap<String, Object>();
        attributes.put("id", id);
        attributes.put("login", login);
        if (email != null) {
            attributes.put("email", email);
        }
        if (name != null) {
            attributes.put("name", name);
        }

        return new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("SCOPE_user:email")),
            attributes,
            "id"
        );
    }

    private User makeUser(Long id, String name, String email,
                          AuthProvider authProvider, String providerId) {
        var user = new User(name, email, "secret");
        user.setRole(Role.USER);
        user.setAuthProvider(authProvider);
        user.setProviderId(providerId);
        setField(User.class, user, "id", id);
        return user;
    }

    private void setField(Class<?> type, Object target, String fieldName, Object value) {
        try {
            Field field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
