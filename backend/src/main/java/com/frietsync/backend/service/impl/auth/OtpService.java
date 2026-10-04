package com.frietsync.backend.service.impl.auth;

import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.exception.RateLimitExceededException;
import com.frietsync.backend.entity.auth.OtpPurpose;
import com.frietsync.backend.entity.auth.OtpVerificationResult;
import com.frietsync.backend.service.impl.email.EmailServiceImpl;
import com.frietsync.backend.service.impl.ratelimit.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final StringRedisTemplate redisTemplate;
    private final EmailServiceImpl emailService;
    private final RateLimitService rateLimitService;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final Duration REQUEST_WINDOW = Duration.ofMinutes(15);
    private static final Duration VERIFICATION_WINDOW = Duration.ofMinutes(1);

    public void enforceRequestRateLimit(String email, String ipAddress) {
        enforceRateLimit(email, ipAddress, "request", 3, 20, REQUEST_WINDOW);
    }

    public void enforceVerificationRateLimit(String email, String ipAddress) {
        enforceRateLimit(email, ipAddress, "verification", 10, 30, VERIFICATION_WINDOW);
    }

    private void enforceRateLimit(
            String email, String ipAddress, String operation, int emailLimit, int ipLimit, Duration window) {
        String normalizedEmail = normalizeEmail(email);
        String emailKey = "otp-" + operation + ":email:" + hash(normalizedEmail);
        String ipKey = "otp-" + operation + ":ip:" + hash(ipAddress);

        boolean emailAllowed = rateLimitService.isAllowed(emailKey, emailLimit, window);
        boolean ipAllowed = rateLimitService.isAllowed(ipKey, ipLimit, window);
        if (!emailAllowed || !ipAllowed) {
            throw new RateLimitExceededException("Too many OTP requests. Please try again later.");
        }
    }

    public void sendOtp(String email, OtpPurpose purpose) {
        email = normalizeEmail(email);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set(otpKey(email, purpose), code, OTP_TTL);
        redisTemplate.delete(attemptsKey(email, purpose));
        emailService.sendOtpMail(email, code);
    }

    public void verifyOtp(String email, String code, OtpPurpose purpose, String ipAddress) {
        email = normalizeEmail(email);
        enforceVerificationRateLimit(email, ipAddress);
        OtpVerificationResult verificationResult = verifyStoredOtp(email, code, purpose);
        switch (verificationResult) {
            case SUCCESS -> {
                return;
            }
            case EXPIRED -> throw new BadRequestException("OTP has expired or was not found");
            case TOO_MANY_ATTEMPTS -> throw new BadRequestException(
                    "Too many invalid OTP attempts. Request a new code.");
            case INVALID -> throw new BadRequestException("Invalid OTP");
        }
    }

    private OtpVerificationResult verifyStoredOtp(String email, String code, OtpPurpose purpose) {
        String otpKey = otpKey(email, purpose);
        String attemptsKey = attemptsKey(email, purpose);
        String savedCode = redisTemplate.opsForValue().get(otpKey);
        if (savedCode == null) {
            return OtpVerificationResult.EXPIRED;
        }
        if (savedCode.equals(code)) {
            redisTemplate.delete(otpKey);
            redisTemplate.delete(attemptsKey);
            return OtpVerificationResult.SUCCESS;
        }

        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts == null) {
            throw new IllegalStateException("Redis did not increment OTP attempt count");
        }
        if (attempts == 1) {
            redisTemplate.expire(attemptsKey, OTP_TTL);
        }
        if (attempts >= MAX_ATTEMPTS) {
            redisTemplate.delete(otpKey);
            redisTemplate.delete(attemptsKey);
            return OtpVerificationResult.TOO_MANY_ATTEMPTS;
        }
        return OtpVerificationResult.INVALID;
    }

    private String otpKey(String email, OtpPurpose purpose) {
        return "otp:" + purpose + ":" + email;
    }

    private String attemptsKey(String email, OtpPurpose purpose) {
        return "otp-attempts:" + purpose + ":" + email;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}