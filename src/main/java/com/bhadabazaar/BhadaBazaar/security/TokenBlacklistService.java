package com.bhadabazaar.BhadaBazaar.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of revoked (logged-out) JWTs. Each entry maps a token to its own expiry, so an
 * entry only needs to live until the token would have expired naturally; a scheduled sweep purges
 * the rest.
 *
 * Note: this state is per-instance and is lost on restart. For a single deployment that is
 * acceptable (a restart simply means revoked tokens are honoured again until their natural 24h
 * expiry). If this app is ever scaled horizontally, the blacklist must move to a shared store
 * (e.g. Redis or a DB table) so revocation is seen by every instance.
 */
@Service
public class TokenBlacklistService {

    private final Map<String, Date> blacklist = new ConcurrentHashMap<>();

    /** Revokes a token until the given expiry. */
    public void blacklist(String token, Date expiry) {
        if (token != null && expiry != null) {
            blacklist.put(token, expiry);
        }
    }

    public boolean isBlacklisted(String token) {
        return token != null && blacklist.containsKey(token);
    }

    /** Removes entries whose tokens have already expired; runs hourly. */
    @Scheduled(fixedRate = 3_600_000)
    public void purgeExpired() {
        Date now = new Date();
        blacklist.entrySet().removeIf(entry -> entry.getValue().before(now));
    }
}
