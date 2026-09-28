package com.frietsync.backend.common.mail;

public interface EmailService {
    void sendSimpleMail(EmailDetails details);
    void sendInviteMail(String recipient, String role);
}