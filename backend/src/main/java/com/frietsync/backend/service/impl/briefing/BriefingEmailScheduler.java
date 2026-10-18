package com.frietsync.backend.service.impl.briefing;

import com.frietsync.backend.dto.briefing.DailyBriefingResponse;
import com.frietsync.backend.dto.issue.IssueSummaryResponse;
import com.frietsync.backend.entity.briefing.BriefingPreference;
import com.frietsync.backend.entity.user.User;
import com.frietsync.backend.repository.briefing.BriefingPreferenceRepository;
import com.frietsync.backend.repository.user.UserRepository;
import com.frietsync.backend.service.briefing.BriefingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class BriefingEmailScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(BriefingEmailScheduler.class);

    private final BriefingPreferenceRepository preferenceRepository;
    private final BriefingService briefingService;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Scheduled(cron = "0 * * * * *")
    public void sendDueBriefings() {
        List<BriefingPreference> preferences = preferenceRepository.findByEmailEnabledTrue();
        for (BriefingPreference preference : preferences) {
            try {
                sendIfDue(preference);
            } catch (Exception ex) {
                LOG.warn("Could not send the daily briefing to user {}: {}", preference.getUserId(),
                        ex.getMessage());
            }
        }
    }

    private void sendIfDue(BriefingPreference preference) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(preference.getTimeZone()));
        LocalDate today = now.toLocalDate();

        if (today.equals(preference.getLastSentOn())) {
            return;
        }
        if (now.toLocalTime().isBefore(preference.getDeliveryTime())) {
            return;
        }

        User user = userRepository.findById(preference.getUserId()).orElse(null);
        if (user == null || !user.isActive()) {
            return;
        }

        DailyBriefingResponse briefing = briefingService.today(preference.getUserId());

        preference.setLastSentOn(today);
        preferenceRepository.save(preference);

        SimpleMailMessage message = new SimpleMailMessage();
        if (fromAddress != null && !fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }
        message.setTo(user.getEmail());
        message.setSubject("Your daily briefing for " + briefing.getDate());
        message.setText(emailBody(briefing));
        mailSender.send(message);
    }

    private String emailBody(DailyBriefingResponse briefing) {
        StringBuilder body = new StringBuilder();
        body.append("Hi ").append(briefing.getUserName()).append(",\n\n");
        body.append(briefing.getHeadline()).append("\n");
        addSection(body, "Overdue", briefing.getOverdue());
        addSection(body, "Due today", briefing.getDueToday());
        addSection(body, "Due soon", briefing.getDueSoon());
        body.append("\nOpen issues: ").append(briefing.getOpenIssues()).append("\n");
        body.append("Unread notifications: ").append(briefing.getUnreadNotifications()).append("\n");
        return body.toString();
    }

    private void addSection(StringBuilder body, String title, List<IssueSummaryResponse> issues) {
        if (issues.isEmpty()) {
            return;
        }
        body.append("\n").append(title).append(":\n");
        for (IssueSummaryResponse issue : issues) {
            body.append("- #").append(issue.getIssueNumber()).append(" ").append(issue.getTitle())
                    .append(" (due ").append(issue.getDueDate()).append(")\n");
        }
    }
}