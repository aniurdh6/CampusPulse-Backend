package com.campuspulse.security;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/** Keeps session tokens in memory. Restarting the server signs everyone out. */
@Component
public class TokenStore {

    private record Entry(Long userId, Instant expires) {}

    private static final Duration TTL = Duration.ofHours(12);

    private final Map<String, Entry> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String issue(Long userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Entry(userId, Instant.now().plus(TTL)));
        return token;
    }

    public Optional<Long> resolve(String token) {
        Entry entry = sessions.get(token);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expires().isBefore(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(entry.userId());
    }

    public void revoke(String token) {
        sessions.remove(token);
    }
}
