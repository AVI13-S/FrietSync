package com.frietsync.backend.service.impl.auth;

import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.service.impl.email.EmailServiceImpl;
import com.frietsync.backend.entity.auth.OtpPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final StringRedisTemplate redisTemplate;
    private final EmailServiceImpl emailService;

    private static final SecureRandom RANDOM = new SecureRandom();

    public void sendOtp(String email, OtpPurpose purpose) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String key = "otp:" + purpose + ":" + email;
        redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(10));
        emailService.sendOtpMail(email, code);
    }

    public void verifyOtp(String email, String code, OtpPurpose purpose) {
        String key = "otp:" + purpose + ":" + email;
        String savedCode = redisTemplate.opsForValue().get(key);

        if (savedCode == null) {
            throw new BadRequestException("OTP has expired or was not found");
        }

        if (!savedCode.equals(code)) {
            throw new BadRequestException("Invalid OTP");
        }

        redisTemplate.delete(key);
    }
}