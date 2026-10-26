package com.frietsync.backend.repository.poll;

import com.frietsync.backend.entity.poll.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PollOptionRepository extends JpaRepository<PollOption, UUID> {
    List<PollOption> findByPollIdOrderByOptionOrder(UUID pollId);

    List<PollOption> findByPollIdInOrderByOptionOrder(List<UUID> pollIds);
}