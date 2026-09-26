package com.frietsync.backend.common.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    @Override
    public void sendSimpleMail(EmailDetails details) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(details.getRecipient());
        mailMessage.setSubject(details.getSubject());
        mailMessage.setText(details.getMsgBody());
        javaMailSender.send(mailMessage);
    }

    public void sendOtpMail(String recipient, String otp) {
        EmailDetails details = new EmailDetails();
        details.setRecipient(recipient);
        details.setSubject("Your FrietSync verification code");
        details.setMsgBody("Your OTP is: " + otp + "\nThis code expires in 10 minutes.");
        sendSimpleMail(details);
    }
}