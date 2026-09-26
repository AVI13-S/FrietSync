package com.frietsync.backend.user.service;

import com.frietsync.backend.user.dto.AuthResponse;
import com.frietsync.backend.user.dto.LoginRequest;
import com.frietsync.backend.user.dto.SignupRequest;
import com.frietsync.backend.user.dto.UserResponse;

public interface AuthService {
    UserResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
}