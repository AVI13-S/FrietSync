package com.frietsync.backend.repository.poll;

import com.frietsync.backend.entity.poll.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PollVoteRepository extends JpaRepository<PollVote, UUID> {
    List<PollVote> findByPollIdAndUserId(UUID pollId, UUID userId);
    List<PollVote> findByPollId(UUID pollId);
    void deleteByPollIdAndUserId(UUID pollId, UUID userId);
}
