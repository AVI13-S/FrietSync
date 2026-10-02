package com.frietsync.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank
    private String email;

    @NotBlank
    private String code;

    @NotBlank
    @Size(min = 6, max = 20)
    @Pattern(regexp = "^[\\p{L}\\p{N}\\p{P}\\p{S}]+$",
            message = "Password can contain letters, numbers, and symbols only")
    private String newPassword;
}
