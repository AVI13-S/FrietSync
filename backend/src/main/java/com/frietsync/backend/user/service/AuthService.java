package com.frietsync.backend.user.service;

import com.frietsync.backend.user.dto.*;

public interface AuthService {
    UserResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
    void verifySignupOtp(VerifyOtpRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    String refreshAccessToken(RefreshRequest request);
    void logout(RefreshRequest request);
}