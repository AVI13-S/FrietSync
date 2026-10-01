package com.frietsync.backend.service.auth;

import com.frietsync.backend.dto.auth.*;
import com.frietsync.backend.dto.user.UserResponse;

public interface AuthService {
    UserResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshAccessToken(RefreshRequest request);
    void verifySignupOtp(VerifyOtpRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    void logout(RefreshRequest request);
}