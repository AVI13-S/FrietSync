package com.frietsync.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank
    @Pattern(
            regexp = "^\\d{6}$",
            message = "OTP must be exactly 6 digits"
    )
    private String code;
}