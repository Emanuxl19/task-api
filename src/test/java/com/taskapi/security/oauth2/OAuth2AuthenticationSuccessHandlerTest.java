package com.taskapi.security.oauth2;

import com.taskapi.entity.AuthProvider;
import com.taskapi.entity.Role;
import com.taskapi.entity.User;
import com.taskapi.repository.UserRepository;
import com.taskapi.security.auth.AuthDTO.TokenResponse;
import com.taskapi.security.auth.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2AuthenticationSuccessHandler")
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    UserRepository userRepository;

    @Mock
    AuthService authService;

    private final OAuth2UserInfoExtractorFactory extractorFactory =
        new OAuth2UserInfoExtractorFactory(List.of(
            new GoogleUserInfoExtractor(),
            new GitHubUserInfoExtractor()
        ));

    @Test
    @DisplayName("redirects to the SPA with issued tokens")
    void shouldRedirectToSpaWithTokens() throws Exception {
        var properties = new OAuth2Properties("http://localhost:3000/oauth2/callback");
        var handler = new OAuth2AuthenticationSuccessHandler(
            extractorFactory,
            userRepository,
            authService,
            properties
        );

        var user = makeUser(1L, "Ana", "ana@email.com", AuthProvider.GOOGLE, "google-1");
        var principal = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            Map.of("sub", "google-1", "email", "ana@email.com", "name", "Ana"),
            "sub"
        );
        var authentication = new OAuth2AuthenticationToken(
            principal,
            principal.getAuthorities(),
            "google"
        );
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        when(userRepository.findByProviderIdAndAuthProvider("google-1", AuthProvider.GOOGLE))
            .thenReturn(Optional.of(user));
        when(authService.issueTokens(user))
            .thenReturn(TokenResponse.of("access.token", "refresh-token", 900_000));

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl())
            .isEqualTo("http://localhost:3000/oauth2/callback?accessToken=access.token&refreshToken=refresh-token&tokenType=Bearer&expiresIn=900");
        verify(authService).issueTokens(user);
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
