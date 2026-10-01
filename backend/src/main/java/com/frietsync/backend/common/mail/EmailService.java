package com.frietsync.backend.common.mail;

public interface EmailService {
    void sendSimpleMail(EmailDetails details);
    void sendOtpMail(String recipient, String otp);
    void sendInviteMail(String recipient, String role);
}