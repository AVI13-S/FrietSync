package com.frietsync.backend.service.email;

import com.frietsync.backend.dto.email.EmailDetails;

public interface EmailService {
    void sendSimpleMail(EmailDetails details);
    void sendOtpMail(String recipient, String otp);
    void sendInviteMail(String recipient, String role);
}