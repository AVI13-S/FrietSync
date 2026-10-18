package com.frietsync.backend.entity.briefing;

import com.frietsync.backend.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "briefing_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id"}))
public class BriefingPreference extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "delivery_time", nullable = false)
    private LocalTime deliveryTime = LocalTime.of(9, 0);

    @Column(name = "time_zone", nullable = false, length = 50)
    private String timeZone = "UTC";

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = false;

    @Column(name = "last_sent_on")
    private LocalDate lastSentOn;
}