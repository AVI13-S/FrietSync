package com.frietsync.backend.dto.auth;

import com.frietsync.backend.dto.user.UserResponse;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private UserResponse user;
    private String accessToken;

    @JsonIgnore
    private String refreshToken;
}