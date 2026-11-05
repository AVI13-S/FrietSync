package com.frietsync.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserSkillRequest {

    @NotBlank(message = "Skill name must not be blank")
    @Size(max = 100, message = "Skill name must not exceed 100 characters")
    private String skillName;
}
