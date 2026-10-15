package com.frietsync.backend.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 2000)
    private String bio;

    @Size(max = 120)
    private String jobTitle;

    @Size(max = 500)
    private String githubUrl;

    @Size(max = 500)
    private String linkedinUrl;

    @Size(max = 500)
    private String portfolioUrl;
}
