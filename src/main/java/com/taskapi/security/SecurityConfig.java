package com.taskapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskapi.security.ratelimit.RateLimitFilter;
import com.taskapi.security.ratelimit.RateLimitProperties;
import com.taskapi.security.ratelimit.RateLimitStore;
import com.taskapi.security.auth.CustomUserDetailsService;
import com.taskapi.security.jwt.JwtAuthenticationFilter;
import com.taskapi.security.jwt.JwtTokenProvider;
import com.taskapi.security.oauth2.CustomOAuth2UserService;
import com.taskapi.security.oauth2.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final CorsProperties corsProperties;
    private final RateLimitProperties rateLimitProperties;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider,
                          CustomUserDetailsService userDetailsService,
                          CorsProperties corsProperties,
                          RateLimitProperties rateLimitProperties,
                          CustomOAuth2UserService customOAuth2UserService,
                          OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.corsProperties = corsProperties;
        this.rateLimitProperties = rateLimitProperties;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oauth2AuthenticationSuccessHandler = oauth2AuthenticationSuccessHandler;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @Bean
    public RateLimitFilter rateLimitFilter(ObjectMapper objectMapper, RateLimitStore rateLimitStore) {
        return new RateLimitFilter(rateLimitProperties, jwtTokenProvider, objectMapper, rateLimitStore);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           RateLimitFilter rateLimitFilter,
                                           JwtAuthenticationFilter jwtAuthenticationFilter,
                                           ObjectMapper objectMapper) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new RestAuthenticationEntryPoint(objectMapper),
                    request -> request.getServletPath().startsWith("/api/")
                )
                .accessDeniedHandler(new RestAccessDeniedHandler(objectMapper)))
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService))
                .successHandler(oauth2AuthenticationSuccessHandler))
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .contentTypeOptions(content -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                 "/api-docs/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                // User management — admin only (registration goes through /auth/register)
                .requestMatchers(HttpMethod.GET, "/api/v1/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/users/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/{id}").hasRole("ADMIN")
                // /users/me — any authenticated user
                .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                // /users/{id} — any authenticated user (service layer checks ownership)
                .requestMatchers(HttpMethod.GET, "/api/v1/users/{id}").authenticated()
                // All other requests need authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(jwtAuthenticationFilter, RateLimitFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.allowedOrigins());
        config.setAllowedMethods(corsProperties.allowedMethods());
        config.setAllowedHeaders(corsProperties.allowedHeaders());
        config.setAllowCredentials(corsProperties.allowCredentials());
        config.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
