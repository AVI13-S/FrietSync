package com.frietsync.backend.controller.auth;

import com.frietsync.backend.dto.auth.*;
import com.frietsync.backend.service.auth.AuthService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    private final AuthService authService;

    @Value("${app.auth.refresh-cookie.secure:false}")
    private boolean refreshCookieSecure;

    @Value("${app.auth.refresh-cookie.same-site:Lax}")
    private String refreshCookieSameSite;

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(
            @Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
        authService.signup(request, httpRequest.getRemoteAddr());
        return ResponseEntity.accepted().body(Map.of(
                "message", "If this email can be registered, verification instructions will be sent."));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request, HttpServletRequest httpRequest) {
        authService.resendSignupOtp(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of(
                "message", "If this account requires verification, a new code will be sent."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return withRefreshCookie(response, response.getRefreshToken());
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request, HttpServletRequest httpRequest) {
        authService.verifySignupOtp(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of("message", "Account verified successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        authService.forgotPassword(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of(
                "message", "If an account exists for this email, password reset instructions will be sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest) {
        authService.resetPassword(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        AuthResponse response = authService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        buildRefreshCookie(response.getRefreshToken(), REFRESH_TOKEN_TTL).toString())
                .body(new AccessTokenResponse(response.getAccessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie("", Duration.ZERO).toString())
                .body(Map.of("message", "Logged out successfully"));
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(AuthResponse response, String refreshToken) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshToken, REFRESH_TOKEN_TTL).toString())
                .body(response);
    }

    private ResponseCookie buildRefreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}