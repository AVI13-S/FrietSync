package com.frietsync.backend.entity.user;

import com.frietsync.backend.entity.common.BaseEntity;
import com.frietsync.backend.entity.user.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "password_version", nullable = false, columnDefinition = "integer default 1")
    private Integer passwordVersion = 1;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 2000)
    private String bio;

    @Column(name = "job_title", length = 200)
    private String jobTitle;

    @Column(name = "github_url", length = 200)
    private String githubUrl;

    @Column(name = "linkedin_url", length = 200)
    private String linkedinUrl;

    @Column(name = "portfolio_url", length = 200)
    private String portfolioUrl;

    @Column(name = "profile_picture_key", length = 2000)
    private String profilePictureKey;
}
