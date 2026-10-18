package com.frietsync.backend.repository.briefing;

import com.frietsync.backend.entity.briefing.BriefingPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BriefingPreferenceRepository extends JpaRepository<BriefingPreference, UUID> {

    Optional<BriefingPreference> findByUserId(UUID userId);

    List<BriefingPreference> findByEmailEnabledTrue();
}