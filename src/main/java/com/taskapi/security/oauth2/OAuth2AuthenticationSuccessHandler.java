package com.taskapi.security.oauth2;

import com.taskapi.entity.User;
import com.taskapi.repository.UserRepository;
import com.taskapi.security.auth.AuthDTO.TokenResponse;
import com.taskapi.security.auth.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2UserInfoExtractorFactory extractorFactory;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final OAuth2Properties properties;
    private final OAuth2AuthorizationCodeStore codeStore;

    public OAuth2AuthenticationSuccessHandler(OAuth2UserInfoExtractorFactory extractorFactory,
                                              UserRepository userRepository,
                                              @Lazy AuthService authService,
                                              OAuth2Properties properties,
                                              OAuth2AuthorizationCodeStore codeStore) {
        this.extractorFactory = extractorFactory;
        this.userRepository = userRepository;
        this.authService = authService;
        this.properties = properties;
        this.codeStore = codeStore;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2UserInfoExtractor extractor = extractorFactory.getExtractor(registrationId);
        OAuth2UserInfoExtractor.OAuth2UserInfo userInfo =
            extractor.extract(oauthToken.getPrincipal().getAttributes());

        User user = userRepository.findByProviderIdAndAuthProvider(
                userInfo.providerId(),
                userInfo.authProvider()
            )
            .or(() -> userRepository.findByEmail(userInfo.email()))
            .orElseThrow(() -> new IllegalStateException(
                "OAuth2 user was not found after successful authentication"
            ));

        // Tokens are issued and stashed against a one-time code; only the
        // code is sent back via the redirect URL. The frontend swaps it for
        // tokens at POST /api/v1/auth/oauth2/exchange.
        TokenResponse tokens = authService.issueTokens(user);
        String code = codeStore.issue(tokens);

        String redirectUrl = UriComponentsBuilder
            .fromUriString(properties.redirectUri())
            .queryParam("code", code)
            .build()
            .encode()
            .toUriString();

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
