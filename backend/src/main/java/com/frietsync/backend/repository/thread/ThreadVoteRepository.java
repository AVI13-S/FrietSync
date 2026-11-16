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

    @Modifying
    @Query(value = "insert into thread_votes (id, thread_id, user_id, created_at, updated_at) "
            + "values (:id, :threadId, :userId, current_timestamp, current_timestamp) "
            + "on conflict (thread_id, user_id) do nothing", nativeQuery = true)
    int insertVoteIfAbsent(@Param("id") UUID id,
                           @Param("threadId") UUID threadId,
                           @Param("userId") UUID userId);

    @Modifying(flushAutomatically = true)
    @Query("delete from ThreadVote v where v.threadId = :threadId and v.userId = :userId")
    int deleteVote(@Param("threadId") UUID threadId, @Param("userId") UUID userId);

    @Query("select v.threadId from ThreadVote v where v.userId = :userId and v.threadId in :threadIds")
    List<UUID> findVotedThreadIds(@Param("userId") UUID userId, @Param("threadIds") Collection<UUID> threadIds);
}