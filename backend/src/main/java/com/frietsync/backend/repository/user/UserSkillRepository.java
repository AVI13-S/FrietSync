package com.frietsync.backend.repository.user;

import com.frietsync.backend.entity.user.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, UUID> {
    List<UserSkill> findByUserId(UUID userId);
    long countByUserId(UUID userId);
    boolean existsByUserIdAndSkillNameIgnoreCase(UUID userId, String skillName);
}
