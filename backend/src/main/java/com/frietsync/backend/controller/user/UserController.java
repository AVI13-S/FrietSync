package com.frietsync.backend.controller.user;

import com.frietsync.backend.dto.user.ProfileResponse;
import com.frietsync.backend.dto.user.ProfileUpdateRequest;
import com.frietsync.backend.dto.user.UserSkillRequest;
import com.frietsync.backend.dto.user.UserSkillResponse;
import com.frietsync.backend.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PatchMapping("/me")
    public ResponseEntity<ProfileResponse> updateMyProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @PostMapping(value = "/me/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileResponse> uploadProfilePicture(
            @AuthenticationPrincipal UUID userId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.uploadProfilePicture(userId, file));
    }

    @DeleteMapping("/me/profile-picture")
    public ResponseEntity<ProfileResponse> removeProfilePicture(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userService.removeProfilePicture(userId));
    }

    @DeleteMapping("/workspace/leave")
    public ResponseEntity<Map<String, String>> leaveWorkspace(
            @AuthenticationPrincipal UUID userId) {

        userService.leaveWorkspace(userId);

        return ResponseEntity.ok(
                Map.of("message", "Successfully left the workspace")
        );
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal UUID userId) {
        userService.deleteAccount(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/skills")
    public ResponseEntity<List<UserSkillResponse>> getMySkills(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userService.getUserSkills(userId));
    }

    @PostMapping("/me/skills")
    public ResponseEntity<UserSkillResponse> addUserSkill(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UserSkillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.addUserSkill(userId, request));
    }

    @DeleteMapping("/me/skills/{skillId}")
    public ResponseEntity<Void> deleteUserSkill(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID skillId) {
        userService.deleteUserSkill(userId, skillId);
        return ResponseEntity.noContent().build();
    }
}
