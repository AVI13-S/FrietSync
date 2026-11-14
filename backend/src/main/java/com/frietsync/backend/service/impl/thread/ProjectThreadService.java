package com.frietsync.backend.service.impl.thread;

import com.frietsync.backend.dto.thread.CommentResponse;
import com.frietsync.backend.dto.thread.CreateCommentRequest;
import com.frietsync.backend.dto.thread.CreateThreadRequest;
import com.frietsync.backend.dto.thread.ThreadDetailResponse;
import com.frietsync.backend.dto.thread.ThreadSummaryResponse;
import com.frietsync.backend.entity.project.Project;
import com.frietsync.backend.entity.thread.ProjectThread;
import com.frietsync.backend.entity.thread.ThreadComment;
import com.frietsync.backend.entity.thread.ThreadVote;
import com.frietsync.backend.entity.user.User;
import com.frietsync.backend.exception.BadRequestException;
import com.frietsync.backend.exception.ResourceNotFoundException;
import com.frietsync.backend.repository.thread.ProjectThreadRepository;
import com.frietsync.backend.repository.thread.ThreadCommentRepository;
import com.frietsync.backend.repository.thread.ThreadVoteRepository;
import com.frietsync.backend.repository.user.UserRepository;
import com.frietsync.backend.service.impl.project.ProjectAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectThreadService {

    private static final String UNKNOWN_USER = "Unknown User";

    private final ProjectAccess projectAccess;
    private final ProjectThreadRepository threadRepository;
    private final ThreadCommentRepository commentRepository;
    private final ThreadVoteRepository voteRepository;
    private final UserRepository userRepository;


    @Transactional(readOnly = true)
    public Page<ThreadSummaryResponse> getThreads(UUID projectId, Pageable pageable, UUID userId) {
        Project project = projectAccess.project(projectId);
        projectAccess.requireMember(project, userId);

        Page<ProjectThread> page =
                threadRepository.findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(projectId, pageable);

        List<ProjectThread> threads = page.getContent();
        Map<UUID, String> names = loadUserNames(threads.stream().map(ProjectThread::getAuthorId).toList());
        Set<UUID> votedIds = threads.isEmpty()
                ? Set.of()
                : new HashSet<>(voteRepository.findVotedThreadIds(
                userId, threads.stream().map(ProjectThread::getId).toList()));

        return page.map(t -> toSummary(t, names, votedIds.contains(t.getId())));
    }

    @Transactional
    public ThreadSummaryResponse createThread(UUID projectId, CreateThreadRequest request, UUID userId) {
        Project project = projectAccess.project(projectId);
        projectAccess.requireMember(project, userId);
        projectAccess.requireActiveProject(project);

        ProjectThread thread = new ProjectThread();
        thread.setProjectId(projectId);
        thread.setAuthorId(userId);
        thread.setTitle(request.getTitle().trim());
        thread.setContent(request.getContent().trim());
        thread.setUpvotes(0);
        thread = threadRepository.save(thread);

        return toSummary(thread, loadUserNames(List.of(userId)), false);
    }

    @Transactional(readOnly = true)
    public ThreadDetailResponse getThreadDetail(UUID projectId, UUID threadId, UUID userId) {
        Project project = projectAccess.project(projectId);
        projectAccess.requireMember(project, userId);

        ProjectThread thread = getActiveThread(projectId, threadId);
        List<ThreadComment> comments =
                commentRepository.findByThreadIdAndDeletedAtIsNullOrderByCreatedAtAsc(threadId);

        List<UUID> authorIds = new java.util.ArrayList<>();
        authorIds.add(thread.getAuthorId());
        comments.forEach(c -> authorIds.add(c.getAuthorId()));
        Map<UUID, String> names = loadUserNames(authorIds);

        ThreadDetailResponse response = new ThreadDetailResponse();
        response.setId(thread.getId());
        response.setProjectId(thread.getProjectId());
        response.setAuthorId(thread.getAuthorId());
        response.setAuthorName(names.getOrDefault(thread.getAuthorId(), UNKNOWN_USER));
        response.setTitle(thread.getTitle());
        response.setContent(thread.getContent());
        response.setUpvotes(thread.getUpvotes());
        response.setUpvotedByMe(voteRepository.existsByThreadIdAndUserId(threadId, userId));
        response.setCreatedAt(thread.getCreatedAt());
        response.setUpdatedAt(thread.getUpdatedAt());
        response.setComments(comments.stream().map(c -> toCommentResponse(c, names)).toList());
        return response;
    }

    @Transactional
    public void deleteThread(UUID projectId, UUID threadId, UUID userId) {
        Project project = projectAccess.project(projectId);
        projectAccess.requireMember(project, userId);
        projectAccess.requireActiveProject(project);

        ProjectThread thread = getActiveThread(projectId, threadId);

        if (!thread.getAuthorId().equals(userId)) {
            projectAccess.require(project, userId,
                    com.frietsync.backend.entity.user.Role.ADMIN,
                    com.frietsync.backend.entity.user.Role.PROJECT_MANAGER);
        }

        thread.setDeletedAt(Instant.now());
        threadRepository.save(thread);
    }


    @Transactional
    public CommentResponse addComment(UUID projectId, UUID threadId, CreateCommentRequest request, UUID userId) {
        Project project = projectAccess.project(projectId);
        projectAccess.requireMember(project, userId);
        projectAccess.requireActiveProject(project);

        getActiveThread(projectId, threadId);

        if (request.getParentCommentId() != null) {
            ThreadComment parent = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
            if (parent.getDeletedAt() != null || !parent.getThreadId().equals(threadId)) {
                throw new BadRequestException("Invalid parent comment");
            }
        }

        ThreadComment comment = new ThreadComment();
        comment.setThreadId(threadId);
        comment.setParentCommentId(request.getParentCommentId());
        comment.setAuthorId(userId);
        comment.setContent(request.getContent().trim());
        comment.setUpvotes(0);
        comment = commentRepository.save(comment);

        return toCommentResponse(comment, loadUserNames(List.of(userId)));
    }

    @Transactional
    public ThreadSummaryResponse upvoteThread(UUID projectId, UUID threadId, UUID userId) {
        Project project = projectAccess.project(projectId);
        projectAccess.requireMember(project, userId);

        getActiveThread(projectId, threadId);

        boolean votedNow;
        if (voteRepository.deleteVote(threadId, userId) > 0) {
            threadRepository.decrementUpvotes(threadId);
            votedNow = false;
        } else {
            ThreadVote vote = new ThreadVote();
            vote.setThreadId(threadId);
            vote.setUserId(userId);
            try {
                voteRepository.saveAndFlush(vote);
            } catch (DataIntegrityViolationException e) {
                throw new BadRequestException("Vote already registered, please retry");
            }
            threadRepository.incrementUpvotes(threadId);
            votedNow = true;
        }

        ProjectThread fresh = getActiveThread(projectId, threadId);
        return toSummary(fresh, loadUserNames(List.of(fresh.getAuthorId())), votedNow);
    }

    private ProjectThread getActiveThread(UUID projectId, UUID threadId) {
        ProjectThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
        if (thread.getDeletedAt() != null || !thread.getProjectId().equals(projectId)) {
            throw new ResourceNotFoundException("Thread not found");
        }
        return thread;
    }

    private Map<UUID, String> loadUserNames(Collection<UUID> ids) {
        Map<UUID, String> names = new HashMap<>();
        Set<UUID> distinctIds = new HashSet<>(ids);
        if (distinctIds.isEmpty()) {
            return names;
        }
        for (User user : userRepository.findAllById(distinctIds)) {
            String name = user.getName();
            names.put(user.getId(), (name == null || name.isBlank()) ? UNKNOWN_USER : name);
        }
        return names;
    }

    private ThreadSummaryResponse toSummary(ProjectThread t, Map<UUID, String> names, boolean upvotedByMe) {
        ThreadSummaryResponse r = new ThreadSummaryResponse();
        r.setId(t.getId());
        r.setProjectId(t.getProjectId());
        r.setAuthorId(t.getAuthorId());
        r.setAuthorName(names.getOrDefault(t.getAuthorId(), UNKNOWN_USER));
        r.setTitle(t.getTitle());
        r.setContent(t.getContent());
        r.setUpvotes(t.getUpvotes());
        r.setUpvotedByMe(upvotedByMe);
        r.setCreatedAt(t.getCreatedAt());
        r.setUpdatedAt(t.getUpdatedAt());
        return r;
    }

    private CommentResponse toCommentResponse(ThreadComment c, Map<UUID, String> names) {
        CommentResponse cr = new CommentResponse();
        cr.setId(c.getId());
        cr.setThreadId(c.getThreadId());
        cr.setParentCommentId(c.getParentCommentId());
        cr.setAuthorId(c.getAuthorId());
        cr.setAuthorName(names.getOrDefault(c.getAuthorId(), UNKNOWN_USER));
        cr.setContent(c.getContent());
        cr.setUpvotes(c.getUpvotes());
        cr.setCreatedAt(c.getCreatedAt());
        cr.setUpdatedAt(c.getUpdatedAt());
        return cr;
    }
}