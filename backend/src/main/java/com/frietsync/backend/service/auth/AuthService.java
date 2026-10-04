package com.frietsync.backend.service.auth;

import com.frietsync.backend.dto.auth.*;

public interface AuthService {
    void signup(SignupRequest request, String ipAddress);
    void resendSignupOtp(ResendOtpRequest request, String ipAddress);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshAccessToken(String refreshToken);
    void verifySignupOtp(VerifyOtpRequest request, String ipAddress);
    void forgotPassword(ForgotPasswordRequest request, String ipAddress);
    void resetPassword(ResetPasswordRequest request, String ipAddress);
    void logout(String refreshToken);
}