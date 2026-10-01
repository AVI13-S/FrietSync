package com.frietsync.backend.entity.auth;


import com.frietsync.backend.entity.common.BaseEntity;
import com.frietsync.backend.entity.auth.OtpPurpose;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "otps")
@Data
public class Otp extends BaseEntity {

    private String email;
    private String code;

    @Enumerated(EnumType.STRING)
    private OtpPurpose purpose;

    private Instant expiresAt;

    private boolean used = false;
}
