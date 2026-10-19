package com.frietsync.backend.repository.budget;

import com.frietsync.backend.entity.budget.UserSalary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSalaryRepository extends JpaRepository<UserSalary, UUID> {

    Optional<UserSalary> findByUserId(UUID userId);

    List<UserSalary> findByUserIdIn(Collection<UUID> userIds);
}