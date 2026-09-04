package com.aifinance.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class RateLimiterService {

    @Value("${security.rate-limit.login.max-attempts:5}")
    private int maxLoginAttempts;

    @Value("${security.rate-limit.login.duration-minutes:1}")
    private int loginWindowMinutes;

    @Value("${security.rate-limit.ai.max-requests-per-hour:100}")
    private int maxAiRequestsPerHour;

    // IP -> timestamps of login attempts
    private final Map<String, ConcurrentLinkedDeque<Long>> loginAttemptsByIp = new ConcurrentHashMap<>();

    // User/IP -> timestamps of AI requests
    private final Map<String, ConcurrentLinkedDeque<Long>> aiRequestsByKey = new ConcurrentHashMap<>();

    public boolean allowLoginAttempt(String ip) {
        long now = Instant.now().toEpochMilli();
        long windowMillis = loginWindowMinutes * 60L * 1000L;

        ConcurrentLinkedDeque<Long> attempts = loginAttemptsByIp.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());
        purgeOldEntries(attempts, now, windowMillis);

        if (attempts.size() >= maxLoginAttempts) {
            return false;
        }

        attempts.addLast(now);
        return true;
    }

    public boolean allowAiRequest(String userOrIp) {
        long now = Instant.now().toEpochMilli();
        long windowMillis = 60L * 60L * 1000L; // 1 hour

        ConcurrentLinkedDeque<Long> requests = aiRequestsByKey.computeIfAbsent(userOrIp, k -> new ConcurrentLinkedDeque<>());
        purgeOldEntries(requests, now, windowMillis);

        if (requests.size() >= maxAiRequestsPerHour) {
            return false;
        }

        requests.addLast(now);
        return true;
    }

    private void purgeOldEntries(ConcurrentLinkedDeque<Long> deque, long now, long windowMillis) {
        long cutoff = now - windowMillis;
        while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
            deque.pollFirst();
        }
    }
}
