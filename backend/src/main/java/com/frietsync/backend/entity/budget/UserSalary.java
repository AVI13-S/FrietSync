package com.frietsync.backend.entity.budget;

import com.frietsync.backend.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "user_salaries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id"}))
public class UserSalary extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "monthly_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlySalary;
}