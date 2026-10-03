package com.frietsync.backend.entity.auth;

public enum OtpVerificationResult {
    SUCCESS,
    INVALID,
    EXPIRED,
    TOO_MANY_ATTEMPTS
}
