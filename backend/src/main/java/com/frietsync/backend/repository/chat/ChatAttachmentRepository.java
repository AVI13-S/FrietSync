package com.frietsync.backend.repository.chat;

import com.frietsync.backend.entity.chat.ChatAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, UUID> {

    List<ChatAttachment> findByMessageIdIn(Collection<UUID> messageIds);

    List<ChatAttachment> findByMessageIdOrderByCreatedAtAsc(UUID messageId);

    List<ChatAttachment> findByProjectIdAndIdInAndMessageIdIsNull(UUID projectId, Collection<UUID> ids);

    Page<ChatAttachment> findByProjectIdAndMessageIdIsNotNullOrderByCreatedAtDescIdDesc(
            UUID projectId, Pageable pageable);

    List<ChatAttachment> findByProjectId(UUID projectId);
}
