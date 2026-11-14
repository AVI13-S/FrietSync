package com.frietsync.backend.repository.thread;

import com.frietsync.backend.entity.thread.ThreadVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ThreadVoteRepository extends JpaRepository<ThreadVote, UUID> {

    boolean existsByThreadIdAndUserId(UUID threadId, UUID userId);

    @Modifying(flushAutomatically = true)
    @Query("delete from ThreadVote v where v.threadId = :threadId and v.userId = :userId")
    int deleteVote(@Param("threadId") UUID threadId, @Param("userId") UUID userId);

    @Query("select v.threadId from ThreadVote v where v.userId = :userId and v.threadId in :threadIds")
    List<UUID> findVotedThreadIds(@Param("userId") UUID userId, @Param("threadIds") Collection<UUID> threadIds);
}