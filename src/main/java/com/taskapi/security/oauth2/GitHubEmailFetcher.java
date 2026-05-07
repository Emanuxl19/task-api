package com.taskapi.security.oauth2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

/**
 * Fetches the GitHub user's primary verified email via /user/emails.
 *
 * GitHub returns email=null in the regular /user response when the user has
 * "Keep my email addresses private" enabled. The user:email scope (already
 * requested via application.properties) lets us read the full list and pick
 * the primary verified one.
 */
@Component
public class GitHubEmailFetcher {

    private static final Logger log = LoggerFactory.getLogger(GitHubEmailFetcher.class);
    private static final String EMAILS_URL = "https://api.github.com/user/emails";

    private final RestClient restClient;

    public GitHubEmailFetcher() {
        this(RestClient.create());
    }

    GitHubEmailFetcher(RestClient restClient) {
        this.restClient = restClient;
    }

    public Optional<String> fetchPrimaryVerifiedEmail(String accessToken) {
        try {
            List<GitHubEmail> emails = restClient.get()
                .uri(EMAILS_URL)
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(new ParameterizedTypeReference<List<GitHubEmail>>() {});

            if (emails == null) {
                return Optional.empty();
            }
            return emails.stream()
                .filter(GitHubEmail::primary)
                .filter(GitHubEmail::verified)
                .map(GitHubEmail::email)
                .findFirst();
        } catch (Exception ex) {
            log.warn("Failed to fetch GitHub email list: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    record GitHubEmail(String email, boolean primary, boolean verified, String visibility) {}
}
