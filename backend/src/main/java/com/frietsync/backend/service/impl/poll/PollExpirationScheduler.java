package com.frietsync.backend.service.impl.poll;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class PollExpirationScheduler {

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4, task -> {
        Thread thread = new Thread(task, "poll-expiration");
        thread.setDaemon(true);
        return thread;
    });
    private final ConcurrentMap<UUID, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public void schedule(UUID pollId, Instant endsAt, Runnable task) {
        cancel(pollId);
        Duration delay = Duration.between(Instant.now(), endsAt);
        long delayNanos;
        try {
            delayNanos = Math.max(0, delay.toNanos());
        } catch (ArithmeticException exception) {
            delayNanos = Long.MAX_VALUE;
        }
        AtomicReference<ScheduledFuture<?>> futureReference = new AtomicReference<>();
        ScheduledFuture<?> future = executor.schedule(() -> {
            ScheduledFuture<?> runningTask = futureReference.get();
            if (runningTask != null) {
                scheduledTasks.remove(pollId, runningTask);
            }
            task.run();
        }, delayNanos, TimeUnit.NANOSECONDS);
        futureReference.set(future);
        scheduledTasks.put(pollId, future);
    }

    public void cancel(UUID pollId) {
        ScheduledFuture<?> task = scheduledTasks.remove(pollId);
        if (task != null) {
            task.cancel(false);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
