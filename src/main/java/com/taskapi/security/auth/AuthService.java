package com.taskapi.security.auth;

import com.taskapi.entity.RefreshToken;
import com.taskapi.entity.User;
import com.taskapi.exception.EmailAlreadyExistsException;
import com.taskapi.exception.InvalidTokenException;
import com.taskapi.repository.RefreshTokenRepository;
import com.taskapi.repository.UserRepository;
import com.taskapi.security.auth.AuthDTO.*;
import com.taskapi.security.jwt.JwtProperties;
import com.taskapi.security.jwt.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       JwtProperties jwtProperties,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.jwtProperties = jwtProperties;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        var user = new User(request.name(), request.email(),
                            passwordEncoder.encode(request.password()));
        var saved = userRepository.save(user);

        return issueTokens(saved);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        var user = userRepository.findByEmail(request.email())
            .orElseThrow(); // Should never happen after successful authentication

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        Instant now = Instant.now();

        var refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
            .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (!refreshToken.getExpiresAt().isAfter(now)) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidTokenException("Refresh token has expired");
        }

        if (refreshTokenRepository.revokeIfActive(request.refreshToken(), now) == 0) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        // Keep the managed entity consistent with the atomic update above.
        refreshToken.revoke();

        return issueTokens(refreshToken.getUser());
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByToken(request.refreshToken())
            .ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public TokenResponse issueTokens(User user) {
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshTokenValue = tokenProvider.generateRefreshToken();

        var refreshToken = new RefreshToken(
            refreshTokenValue,
            user,
            Instant.now().plusMillis(jwtProperties.refreshTokenExpiration())
        );
        refreshTokenRepository.save(refreshToken);

        return TokenResponse.of(accessToken, refreshTokenValue,
                                jwtProperties.accessTokenExpiration());
    }
}
