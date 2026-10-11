package com.frietsync.backend.service.poll;

import com.frietsync.backend.dto.poll.CreatePollRequest;
import com.frietsync.backend.dto.poll.PollResponse;
import com.frietsync.backend.dto.poll.VoteRequest;
import java.util.List;
import java.util.UUID;

public interface PollService {
    PollResponse create(UUID projectId, CreatePollRequest request, UUID userId);
    List<PollResponse> list(UUID projectId, UUID userId);
    PollResponse get(UUID pollId, UUID userId);
    PollResponse vote(UUID pollId, VoteRequest request, UUID userId);
    PollResponse close(UUID pollId, String decisionRecord, UUID userId);
}
