package com.taskapi.security.oauth2;

import com.taskapi.entity.AuthProvider;
import com.taskapi.entity.User;
import com.taskapi.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuth2UserInfoExtractorFactory extractorFactory;
    private final UserRepository userRepository;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    @Autowired
    public CustomOAuth2UserService(OAuth2UserInfoExtractorFactory extractorFactory,
                                   UserRepository userRepository) {
        this(extractorFactory, userRepository, new DefaultOAuth2UserService());
    }

    CustomOAuth2UserService(OAuth2UserInfoExtractorFactory extractorFactory,
                            UserRepository userRepository,
                            OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate) {
        this.extractorFactory = extractorFactory;
        this.userRepository = userRepository;
        this.delegate = delegate;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        OAuth2UserInfoExtractor extractor = extractorFactory.getExtractor(registrationId);
        OAuth2UserInfoExtractor.OAuth2UserInfo userInfo = extractor.extract(oauth2User.getAttributes());

        validateUserInfo(userInfo);

        User user = userRepository.findByProviderIdAndAuthProvider(
                userInfo.providerId(),
                userInfo.authProvider()
            )
            .map(existing -> updateExistingProviderUser(existing, userInfo))
            .orElseGet(() -> userRepository.findByEmail(userInfo.email())
                .map(existing -> linkExistingUser(existing, userInfo))
                .orElseGet(() -> createUser(userInfo)));

        LinkedHashSet<GrantedAuthority> authorities = new LinkedHashSet<>(oauth2User.getAuthorities());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        return new DefaultOAuth2User(
            authorities,
            buildAttributes(oauth2User.getAttributes(), user),
            extractor.nameAttributeKey()
        );
    }

    private void validateUserInfo(OAuth2UserInfoExtractor.OAuth2UserInfo userInfo) {
        if (isBlank(userInfo.providerId())) {
            throw authenticationException("Provider ID was not returned by the OAuth2 provider");
        }
        if (isBlank(userInfo.email())) {
            throw authenticationException("Email was not returned by the OAuth2 provider");
        }
    }

    private User updateExistingProviderUser(User existingUser,
                                            OAuth2UserInfoExtractor.OAuth2UserInfo userInfo) {
        existingUser.setEmail(userInfo.email());
        existingUser.setName(resolveDisplayName(userInfo));
        existingUser.setProviderId(userInfo.providerId());
        existingUser.setAuthProvider(userInfo.authProvider());
        return userRepository.save(existingUser);
    }

    private User linkExistingUser(User existingUser,
                                  OAuth2UserInfoExtractor.OAuth2UserInfo userInfo) {
        if (existingUser.getAuthProvider() != AuthProvider.LOCAL
                && existingUser.getAuthProvider() != userInfo.authProvider()) {
            throw authenticationException(
                "Account is already linked to " + existingUser.getAuthProvider().name().toLowerCase()
            );
        }

        existingUser.setName(resolveDisplayName(userInfo));
        existingUser.setAuthProvider(userInfo.authProvider());
        existingUser.setProviderId(userInfo.providerId());
        return userRepository.save(existingUser);
    }

    private User createUser(OAuth2UserInfoExtractor.OAuth2UserInfo userInfo) {
        var user = new User(resolveDisplayName(userInfo), userInfo.email());
        user.setAuthProvider(userInfo.authProvider());
        user.setProviderId(userInfo.providerId());
        return userRepository.save(user);
    }

    private Map<String, Object> buildAttributes(Map<String, Object> attributes, User user) {
        var mergedAttributes = new HashMap<>(attributes);
        mergedAttributes.put("appUserId", user.getId());
        mergedAttributes.put("appRole", user.getRole().name());
        return mergedAttributes;
    }

    private String resolveDisplayName(OAuth2UserInfoExtractor.OAuth2UserInfo userInfo) {
        if (!isBlank(userInfo.name())) {
            return userInfo.name();
        }
        int atIndex = userInfo.email().indexOf('@');
        return atIndex > 0 ? userInfo.email().substring(0, atIndex) : userInfo.email();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private OAuth2AuthenticationException authenticationException(String message) {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"), message);
    }
}
