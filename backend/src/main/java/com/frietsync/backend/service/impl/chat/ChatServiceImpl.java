package com.frietsync.backend.service.impl.chat;

import com.frietsync.backend.dto.chat.ChatMessageRequest;
import com.frietsync.backend.dto.chat.ChatMessageResponse;
import com.frietsync.backend.dto.chat.ChatAttachmentResponse;
import com.frietsync.backend.entity.chat.ChatAttachment;
import com.frietsync.backend.entity.chat.ChatMessage;
import com.frietsync.backend.entity.project.Project;
import com.frietsync.backend.entity.user.User;
import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.exception.ForbiddenException;
import com.frietsync.backend.exception.ResourceNotFoundException;
import com.frietsync.backend.repository.chat.ChatMessageRepository;
import com.frietsync.backend.repository.chat.ChatAttachmentRepository;
import com.frietsync.backend.service.chat.ChatAttachmentService;
import com.frietsync.backend.repository.project.ProjectMemberRepository;
import com.frietsync.backend.repository.project.ProjectRepository;
import com.frietsync.backend.repository.user.UserRepository;
import com.frietsync.backend.service.chat.ChatService;
import com.frietsync.backend.service.notification.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatAttachmentRepository chatAttachmentRepository;
    private final ChatAttachmentService chatAttachmentService;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final NotificationPublisher notificationPublisher;

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(UUID projectId, UUID senderId, ChatMessageRequest request) {
        assertProjectAccess(projectId, senderId);
        Project project = projectRepository.findByIdForUpdate(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        String content = request.getContent();
        boolean hasAttachments = request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty();
        if ((!StringUtils.hasText(content) && !hasAttachments)
                || (content != null && content.length() > MAX_MESSAGE_LENGTH)) {
            throw new BadRequestException("Message must contain up to 2000 characters or include an attachment");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ChatMessage replyTo = null;
        if (request.getReplyToId() != null) {
            replyTo = chatMessageRepository.findById(request.getReplyToId())
                    .orElseThrow(() -> new ResourceNotFoundException("Message to reply to not found"));
            if (!replyTo.getProjectId().equals(projectId) || replyTo.isDeleted()) {
                throw new BadRequestException("You can't reply to this message");
            }
        }

        Set<UUID> mentionedIds = new LinkedHashSet<>();
        if (request.getMentionedUserIds() != null) {
            for (UUID mentionedId : request.getMentionedUserIds()) {
                if (mentionedId == null) {
                    continue;
                }
                if (!hasProjectAccess(project, mentionedId)) {
                    throw new BadRequestException("Mentioned user is not a member of this project");
                }
                mentionedIds.add(mentionedId);
            }
        }

        ChatMessage message = new ChatMessage();
        message.setProjectId(projectId);
        message.setSenderId(senderId);
        message.setSenderName(sender.getName());
        message.setContent(content.trim());
        message.setReplyTo(replyTo);
        message.setMentionedUserIds(mentionedIds);
        ChatMessage savedMessage = chatMessageRepository.save(message);
        List<ChatAttachmentResponse> attachments = chatAttachmentService.bindToMessage(
                projectId, senderId, savedMessage.getId(), request.getAttachmentIds());

        Set<UUID> recipients = new LinkedHashSet<>();
        recipients.add(project.getCreatedBy());
        recipients.add(project.getAdminId());
        if (project.getProjectManagerId() != null) {
            recipients.add(project.getProjectManagerId());
        }
        if (project.getTeamLeadId() != null) {
            recipients.add(project.getTeamLeadId());
        }
        projectMemberRepository.findByProjectId(projectId)
                .forEach(member -> recipients.add(member.getUserId()));

        recipients.removeAll(mentionedIds);

        String title = "New message in " + project.getName();
        String notificationMessage = sender.getName() + " sent a message in " + project.getName() + ".";
        recipients.forEach(recipientId -> notificationPublisher.publishChatMessage(
                recipientId, title, notificationMessage, projectId, senderId));

        String mentionTitle = "You were mentioned in " + project.getName();
        String mentionMessage = sender.getName() + " mentioned you in " + project.getName() + ".";
        for (UUID mentionedId : mentionedIds) {
            if (!mentionedId.equals(senderId)) {
                notificationPublisher.publishChatMessage(
                        mentionedId, mentionTitle, mentionMessage, projectId, senderId);
            }
        }

        return ChatMessageResponse.fromEntity(savedMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getRecentMessages(UUID projectId, UUID userId, int limit) {
        assertProjectAccess(projectId, userId);
        List<ChatMessage> messages = new ArrayList<>(chatMessageRepository.findByProjectId(
                projectId,
                PageRequest.of(0, limit, Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                ))
        ));
        Collections.reverse(messages);
        List<UUID> messageIds = messages.stream().map(ChatMessage::getId).toList();
        List<ChatAttachment> attachments = messageIds.isEmpty()
                ? List.of()
                : chatAttachmentRepository.findByMessageIdIn(messageIds);
        java.util.Map<UUID, List<ChatAttachmentResponse>> attachmentsByMessage = attachments.stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                ChatAttachment::getMessageId,
                                java.util.stream.Collectors.mapping(ChatAttachmentResponse::fromEntity,
                                        java.util.stream.Collectors.toList())));
        return messages.stream().map(message -> ChatMessageResponse.fromEntity(
                message, attachmentsByMessage.getOrDefault(message.getId(), List.of()))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void assertProjectAccess(UUID projectId, UUID userId) {
        Project project = findProject(projectId);
        if (!hasProjectAccess(project, userId)) {
            throw new ForbiddenException("You don't have access to this project's chat");
        }
    }

    private boolean hasProjectAccess(Project project, UUID userId) {
        boolean projectRole = userId.equals(project.getCreatedBy())
                || userId.equals(project.getAdminId())
                || userId.equals(project.getProjectManagerId())
                || userId.equals(project.getTeamLeadId());
        return projectRole || projectMemberRepository.existsByProjectIdAndUserId(project.getId(), userId);
    }

    private Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }
}