package com.urlshortener.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.urlshortener.config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight in-memory token-bucket rate limiter, keyed per client (IP or
 * API key). Suitable for a single-instance deployment; for a multi-node
 * cluster, swap this for a Redis-backed limiter (e.g. Bucket4j + Redis) -
 * the {@link #tryConsume} contract stays the same.
 */
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final AppProperties appProperties;

    private Cache<String, Bucket> creationBuckets;
    private Cache<String, Bucket> redirectBuckets;

    @PostConstruct
    void init() {
        creationBuckets = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(10))
                .maximumSize(100_000)
                .build();
        redirectBuckets = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(10))
                .maximumSize(200_000)
                .build();
    }

    public boolean tryConsumeForCreation(String key) {
        if (!appProperties.getRateLimit().isEnabled()) return true;
        var cfg = appProperties.getRateLimit().getCreation();
        Bucket bucket = creationBuckets.get(key, k -> new Bucket(cfg.getCapacity()));
        return bucket.tryConsume(cfg.getCapacity(), cfg.getRefillTokens(), cfg.getRefillDurationSeconds());
    }

    public boolean tryConsumeForRedirect(String key) {
        if (!appProperties.getRateLimit().isEnabled()) return true;
        var cfg = appProperties.getRateLimit().getRedirect();
        Bucket bucket = redirectBuckets.get(key, k -> new Bucket(cfg.getCapacity()));
        return bucket.tryConsume(cfg.getCapacity(), cfg.getRefillTokens(), cfg.getRefillDurationSeconds());
    }

    /** Simple thread-safe token bucket with periodic full refill. */
    private static class Bucket {
        private final AtomicInteger tokens;
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());

        Bucket(int capacity) {
            this.tokens = new AtomicInteger(capacity);
        }

        synchronized boolean tryConsume(int capacity, int refillTokens, int refillDurationSeconds) {
            long now = System.currentTimeMillis();
            long windowMs = refillDurationSeconds * 1000L;
            if (now - windowStart.get() >= windowMs) {
                tokens.set(Math.min(capacity, refillTokens));
                windowStart.set(now);
            }
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }
    }
}
