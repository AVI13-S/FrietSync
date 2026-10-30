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

    @Size(max = 200)
    @ProfileUrl(type = ProfileUrl.Type.GITHUB,
            message = "Enter a GitHub profile link, e.g. https://github.com/username")
    private String githubUrl;

    @Size(max = 200)
    @ProfileUrl(type = ProfileUrl.Type.LINKEDIN,
            message = "Enter a LinkedIn profile link, e.g. https://www.linkedin.com/in/username")
    private String linkedinUrl;

    @Size(max = 200)
    @ProfileUrl(type = ProfileUrl.Type.PORTFOLIO,
            message = "Enter a valid website link that is not a GitHub or LinkedIn profile")
    private String portfolioUrl;
}