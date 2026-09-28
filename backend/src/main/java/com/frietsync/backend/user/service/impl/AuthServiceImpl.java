package com.frietsync.backend.user.service.impl;

import com.frietsync.backend.common.exception.BadRequestException;
import com.frietsync.backend.common.security.JwtUtil;
import com.frietsync.backend.otp.enums.OtpPurpose;
import com.frietsync.backend.otp.service.OtpService;
import com.frietsync.backend.user.dto.*;
import com.frietsync.backend.user.entity.User;
import com.frietsync.backend.user.enums.Role;
import com.frietsync.backend.user.repository.UserRepository;
import com.frietsync.backend.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;

    @Override
    public UserResponse signup(SignupRequest request) {
        User existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (existingUser != null) {
            if (existingUser.isActive()) {
                throw new BadRequestException("Email is already registered");
            }

            otpService.sendOtp(existingUser.getEmail(), OtpPurpose.SIGNUP);
            return UserResponse.fromEntity(existingUser);
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(hashedPassword);
        user.setRole(Role.ADMIN);
        user.setActive(false);

        User savedUser = userRepository.save(user);

        otpService.sendOtp(savedUser.getEmail(), OtpPurpose.SIGNUP);

        return UserResponse.fromEntity(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new BadRequestException("Please verify your email before logging in");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(UserResponse.fromEntity(user), token);
    }

    @Override
    public void verifySignupOtp(VerifyOtpRequest request) {
        otpService.verifyOtp(request.getEmail(), request.getCode(), OtpPurpose.SIGNUP);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        if (!userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("No account found with this email");
        }

        otpService.sendOtp(request.getEmail(), OtpPurpose.RESET_PASSWORD);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        otpService.verifyOtp(request.getEmail(), request.getCode(), OtpPurpose.RESET_PASSWORD);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}