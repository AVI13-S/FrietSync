package com.frietsync.backend.auth.service;

import com.frietsync.backend.user.dto.SignupRequest;
import com.frietsync.backend.user.dto.UserResponse;


public interface AuthService {

    UserResponse signup(SignupRequest request);
}
