package com.frietsync.backend.service.impl.notification;

import com.frietsync.backend.dto.email.EmailDetails;
import com.frietsync.backend.repository.user.UserRepository;
import com.frietsync.backend.service.email.EmailService;
import com.frietsync.backend.service.notification.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEmailListener {

    private final UserRepository userRepository;
    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotification(NotificationEvent event) {
        if (!event.sendEmail()) {
            return;
        }
        try {
            userRepository.findById(event.userId()).ifPresent(user -> {
                EmailDetails details = new EmailDetails();
                details.setRecipient(user.getEmail());
                details.setSubject("FrietSync: " + event.title());
                details.setMsgBody(event.message());
                emailService.sendSimpleMail(details);
            });
        } catch (Exception e) {
            log.warn("Could not send notification email to user {}", event.userId(), e);
        }
    }
}