package com.frietsync.backend.service.user;

import com.frietsync.backend.dto.user.ProfileResponse;
import com.frietsync.backend.dto.user.ProfileUpdateRequest;
import com.frietsync.backend.dto.user.UserSkillRequest;
import com.frietsync.backend.dto.user.UserSkillResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface UserService {
    ProfileResponse getProfile(UUID userId);

    ProfileResponse updateProfile(UUID userId, ProfileUpdateRequest request);

    ProfileResponse uploadProfilePicture(UUID userId, MultipartFile file);

    ProfileResponse removeProfilePicture(UUID userId);

    void leaveWorkspace(UUID userId);

    void deleteAccount(UUID userId);

    List<UserSkillResponse> getUserSkills(UUID userId);

    UserSkillResponse addUserSkill(UUID userId, UserSkillRequest request);

    void deleteUserSkill(UUID userId, UUID skillId);
}
