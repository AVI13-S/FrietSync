package com.frietsync.backend.service.impl.briefing;

import com.frietsync.backend.dto.activity.ActivityResponse;
import com.frietsync.backend.dto.briefing.BriefingMeetingResponse;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class BriefingEmailScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(BriefingEmailScheduler.class);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_ASSIGNED_IN_EMAIL = 10;

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
        String name = briefing.getUserName();
        if (name == null || name.isBlank()) {
            name = "there";
        }

        StringBuilder body = new StringBuilder();
        body.append("Hi ").append(name).append(",\n\n");
        body.append(briefing.getHeadline()).append("\n");

        addSection(body, "Overdue", briefing.getOverdue(), Integer.MAX_VALUE);
        addSection(body, "Due today", briefing.getDueToday(), Integer.MAX_VALUE);
        addSection(body, "Due soon", briefing.getDueSoon(), Integer.MAX_VALUE);
        addSection(body, "New issues (last 24 hours)", briefing.getNewIssues(), Integer.MAX_VALUE);
        addSection(body, "Completed yesterday", briefing.getCompletedYesterday(), Integer.MAX_VALUE);
        addMeetings(body, briefing);
        addUpdates(body, briefing.getProjectUpdates());
        addAlerts(body, briefing.getProjectAlerts());
        addSection(body, "Your open tasks", briefing.getAssignedTasks(), MAX_ASSIGNED_IN_EMAIL);

        body.append("\nOpen issues: ").append(briefing.getOpenIssues()).append("\n");
        if (briefing.getUnreadNotifications() != null) {
            body.append("Unread notifications: ").append(briefing.getUnreadNotifications()).append("\n");
        }
        return body.toString();
    }

    private void addSection(StringBuilder body, String title, List<IssueSummaryResponse> issues, int limit) {
        if (issues == null || issues.isEmpty()) {
            return;
        }
        body.append("\n").append(title).append(":\n");
        int shown = 0;
        for (IssueSummaryResponse issue : issues) {
            if (shown == limit) {
                body.append("- and ").append(issues.size() - limit).append(" more\n");
                break;
            }
            body.append("- #").append(issue.getIssueNumber()).append(" ").append(issue.getTitle());
            if (issue.getDueDate() != null) {
                body.append(" (due ").append(issue.getDueDate()).append(")");
            }
            body.append("\n");
            shown++;
        }
    }

    private void addMeetings(StringBuilder body, DailyBriefingResponse briefing) {
        List<BriefingMeetingResponse> meetings = briefing.getUpcomingMeetings();
        if (meetings == null || meetings.isEmpty()) {
            return;
        }
        ZoneId zone = ZoneId.of(briefing.getTimeZone());
        body.append("\nUpcoming meetings:\n");
        for (BriefingMeetingResponse meeting : meetings) {
            body.append("- ");
            if (meeting.getStartAt() != null) {
                body.append(ZonedDateTime.ofInstant(meeting.getStartAt(), zone).format(TIME)).append(" ");
            }
            body.append(meeting.getTitle()).append("\n");
            if (meeting.getAgenda() != null && !meeting.getAgenda().isBlank()) {
                body.append("  Agenda: ").append(meeting.getAgenda()).append("\n");
            }
        }
    }

    private void addAlerts(StringBuilder body, List<String> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            return;
        }
        body.append("\nProject alerts:\n");
        for (String alert : alerts) {
            body.append("- ").append(alert).append("\n");
        }
    }

    private void addUpdates(StringBuilder body, List<ActivityResponse> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        body.append("\nProject updates (last 24 hours):\n");
        for (ActivityResponse update : updates) {
            body.append("- #").append(update.getIssueNumber()).append(" ").append(update.getIssueTitle())
                    .append(": ").append(update.getActorName()).append(" - ").append(update.getAction());
            if (update.getFieldName() != null) {
                body.append(" ").append(update.getFieldName());
                if (update.getOldValue() != null || update.getNewValue() != null) {
                    body.append(" (").append(update.getOldValue()).append(" -> ")
                            .append(update.getNewValue()).append(")");
                }
            }
            body.append("\n");
        }
    }
}