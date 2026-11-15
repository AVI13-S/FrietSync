package com.frietsync.backend.service.integration;

import com.frietsync.backend.config.integration.GitHubOAuthProperties;
import com.frietsync.backend.entity.integration.GitHubConnection;
import com.frietsync.backend.exception.GitHubReauthorizationRequiredException;
import com.frietsync.backend.repository.integration.GitHubConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GitHubTokenService {

    private static final Duration EXPIRY_SKEW = Duration.ofMinutes(1);
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";

    private final GitHubConnectionRepository connectionRepository;
    private final GitHubOAuthProperties properties;
    private final GitHubTokenCipher cipher;
    private final GitHubApiClient githubApi;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String getValidAccessToken(UUID userId) {
        GitHubConnection connection = connectionRepository.findByUser_Id(userId)
                .orElseThrow(GitHubReauthorizationRequiredException::new);
        if (isAccessTokenValid(connection, Instant.now())) {
            return cipher.decrypt(connection.getEncryptedAccessToken());
        }

        connection = connectionRepository.findByUserIdForUpdate(userId)
                .orElseThrow(GitHubReauthorizationRequiredException::new);
        Instant now = Instant.now();
        if (isAccessTokenValid(connection, now)) {
            return cipher.decrypt(connection.getEncryptedAccessToken());
        }
        return refreshAccessToken(connection, now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String refreshAccessToken(UUID userId) {
        GitHubConnection connection = connectionRepository.findByUserIdForUpdate(userId)
                .orElseThrow(GitHubReauthorizationRequiredException::new);
        return refreshAccessToken(connection, Instant.now());
    }

    private boolean isAccessTokenValid(GitHubConnection connection, Instant now) {
        Instant expiresAt = connection.getAccessTokenExpiresAt();
        return expiresAt == null || expiresAt.isAfter(now.plus(EXPIRY_SKEW));
    }

    private String refreshAccessToken(GitHubConnection connection, Instant now) {
        String encryptedRefreshToken = connection.getEncryptedRefreshToken();
        Instant refreshExpiresAt = connection.getRefreshTokenExpiresAt();
        if (encryptedRefreshToken == null
                || (refreshExpiresAt != null && !refreshExpiresAt.isAfter(now))) {
            throw new GitHubReauthorizationRequiredException();
        }

        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", cipher.decrypt(encryptedRefreshToken));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        Map<String, Object> response;
        try {
            response = githubApi.postForm(TOKEN_URL, new HttpEntity<>(form, headers));
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 400) {
                throw new GitHubReauthorizationRequiredException(ex);
            }
            throw ex;
        }
        if (response == null || !(response.get("access_token") instanceof String accessToken)
                || !(response.get("expires_in") instanceof Number accessExpiresIn)) {
            throw new GitHubReauthorizationRequiredException();
        }

        connection.setEncryptedAccessToken(cipher.encrypt(accessToken));
        connection.setAccessTokenExpiresAt(now.plusSeconds(accessExpiresIn.longValue()));
        if (response.get("refresh_token") instanceof String refreshToken && !refreshToken.isBlank()) {
            connection.setEncryptedRefreshToken(cipher.encrypt(refreshToken));
        }
        if (response.get("refresh_token_expires_in") instanceof Number refreshExpiresIn) {
            connection.setRefreshTokenExpiresAt(now.plusSeconds(refreshExpiresIn.longValue()));
        }
        connectionRepository.save(connection);
        return accessToken;
    }
}
