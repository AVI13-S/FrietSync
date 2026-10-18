package com.frietsync.backend.service.impl.briefing;

import com.frietsync.backend.dto.briefing.BriefingPreferenceResponse;
import com.frietsync.backend.dto.briefing.DailyBriefingResponse;
import com.frietsync.backend.dto.briefing.UpdateBriefingPreferenceRequest;
import com.frietsync.backend.dto.issue.IssueSummaryResponse;
import com.frietsync.backend.entity.briefing.BriefingPreference;
import com.frietsync.backend.entity.issue.Issue;
import com.frietsync.backend.entity.issue.IssueAssignee;
import com.frietsync.backend.entity.issue.IssueStatus;
import com.frietsync.backend.entity.user.User;
import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.repository.briefing.BriefingPreferenceRepository;
import com.frietsync.backend.repository.issue.IssueAssigneeRepository;
import com.frietsync.backend.repository.issue.IssueRepository;
import com.frietsync.backend.repository.user.UserRepository;
import com.frietsync.backend.service.briefing.BriefingService;
import com.frietsync.backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BriefingServiceImpl implements BriefingService {

    private static final int DUE_SOON_DAYS = 3;

    private final IssueRepository issueRepository;
    private final IssueAssigneeRepository assigneeRepository;
    private final BriefingPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public DailyBriefingResponse today(UUID userId) {
        ZoneId zone = zoneOf(userId);
        return build(userId, LocalDate.now(zone), zone);
    }

    @Override
    @Transactional(readOnly = true)
    public DailyBriefingResponse forDate(UUID userId, LocalDate date) {
        ZoneId zone = zoneOf(userId);
        if (date.isAfter(LocalDate.now(zone))) {
            throw new BadRequestException("Date is in the future");
        }
        return build(userId, date, zone);
    }

    @Override
    @Transactional(readOnly = true)
    public BriefingPreferenceResponse getPreferences(UUID userId) {
        BriefingPreference preference = preferenceRepository.findByUserId(userId).orElse(null);
        if (preference == null) {
            preference = new BriefingPreference();
        }
        return toResponse(preference);
    }

    @Override
    @Transactional
    public BriefingPreferenceResponse updatePreferences(UUID userId, UpdateBriefingPreferenceRequest request) {
        if (request.getDeliveryTime() == null && request.getTimeZone() == null
                && request.getEmailEnabled() == null) {
            throw new BadRequestException("Nothing to update");
        }

        BriefingPreference preference = preferenceRepository.findByUserId(userId).orElse(null);
        if (preference == null) {
            preference = new BriefingPreference();
            preference.setUserId(userId);
        }

        if (request.getTimeZone() != null) {
            try {
                ZoneId.of(request.getTimeZone());
            } catch (DateTimeException ex) {
                throw new BadRequestException("Unknown time zone");
            }
            preference.setTimeZone(request.getTimeZone());
        }
        if (request.getDeliveryTime() != null) {
            preference.setDeliveryTime(request.getDeliveryTime().withSecond(0).withNano(0));
        }
        if (request.getEmailEnabled() != null) {
            preference.setEmailEnabled(request.getEmailEnabled());
        }

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(preference.getTimeZone()));
        if (preference.isEmailEnabled() && !now.toLocalTime().isBefore(preference.getDeliveryTime())) {
            preference.setLastSentOn(now.toLocalDate());
        }

        preference = preferenceRepository.save(preference);
        return toResponse(preference);
    }

    private DailyBriefingResponse build(UUID userId, LocalDate date, ZoneId zone) {
        boolean isToday = date.equals(LocalDate.now(zone));

        Instant moment;
        if (isToday) {
            moment = Instant.now();
        } else {
            moment = date.plusDays(1).atStartOfDay(zone).toInstant().minusSeconds(1);
        }

        List<UUID> issueIds = new ArrayList<>();
        for (IssueAssignee assignee : assigneeRepository.findByIssueId(userId)) {
            if (!assignee.getCreatedAt().isAfter(moment)) {
                issueIds.add(assignee.getIssueId());
            }
        }
        List<Issue> issues = issueRepository.findAllById(issueIds);

        List<IssueSummaryResponse> overdue = new ArrayList<>();
        List<IssueSummaryResponse> dueToday = new ArrayList<>();
        List<IssueSummaryResponse> dueSoon = new ArrayList<>();
        int open = 0;
        int completed = 0;
        LocalDate soonLimit = date.plusDays(DUE_SOON_DAYS);

        for (Issue issue : issues) {
            if (issue.getDeletedAt() != null || issue.getCreatedAt().isAfter(moment)) {
                continue;
            }

            Instant finishedAt = finishedAt(issue);
            if (finishedAt != null && !finishedAt.isAfter(moment)) {
                if (finishedAt.atZone(zone).toLocalDate().equals(date)) {
                    completed++;
                }
                continue;
            }

            open++;
            LocalDate due = issue.getDueDate();
            if (due == null) {
                continue;
            }
            IssueSummaryResponse item = toItem(issue, isToday);
            if (due.isBefore(date)) {
                overdue.add(item);
            } else if (due.equals(date)) {
                dueToday.add(item);
            } else if (!due.isAfter(soonLimit)) {
                dueSoon.add(item);
            }
        }

        String userName = null;
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            userName = user.getName();
        }

        Long unread = null;
        if (isToday) {
            unread = notificationService.unreadCount(userId);
        }

        return DailyBriefingResponse.builder()
                .date(date)
                .timeZone(zone.getId())
                .userName(userName)
                .headline(headline(overdue.size(), dueToday.size(), dueSoon.size()))
                .openIssues(open)
                .completedIssues(completed)
                .overdue(overdue)
                .dueToday(dueToday)
                .dueSoon(dueSoon)
                .unreadNotifications(unread)
                .build();
    }

    private String headline(int overdue, int dueToday, int dueSoon) {
        if (overdue + dueToday + dueSoon == 0) {
            return "Nothing needs your attention.";
        }
        return overdue + " overdue, " + dueToday + " due today, " + dueSoon + " due soon.";
    }

    private ZoneId zoneOf(UUID userId) {
        BriefingPreference preference = preferenceRepository.findByUserId(userId).orElse(null);
        if (preference == null) {
            return ZoneId.of("UTC");
        }
        return ZoneId.of(preference.getTimeZone());
    }

    private Instant finishedAt(Issue issue) {
        if (issue.getStatus() != IssueStatus.RESOLVED && issue.getStatus() != IssueStatus.CLOSED) {
            return null;
        }
        if (issue.getResolvedAt() != null) {
            return issue.getResolvedAt();
        }
        return issue.getClosedAt();
    }

    private IssueSummaryResponse toItem(Issue issue, boolean isToday) {
        IssueStatus status = IssueStatus.OPEN;
        if (isToday) {
            status = issue.getStatus();
        }
        return IssueSummaryResponse.builder()
                .id(issue.getId())
                .issueNumber(issue.getIssueNumber())
                .title(issue.getTitle())
                .status(status)
                .priority(issue.getPriority())
                .dueDate(issue.getDueDate())
                .build();
    }

    private BriefingPreferenceResponse toResponse(BriefingPreference preference) {
        return BriefingPreferenceResponse.builder()
                .deliveryTime(preference.getDeliveryTime())
                .timeZone(preference.getTimeZone())
                .emailEnabled(preference.isEmailEnabled())
                .build();
    }
}