
package com.frietsync.backend.repository.auth;

import com.frietsync.backend.entity.auth.Otp;
import com.frietsync.backend.entity.auth.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OtpRepository extends JpaRepository<Otp, UUID> {
    List<Otp> findByEmailAndPurpose(String email, OtpPurpose purpose);
}
