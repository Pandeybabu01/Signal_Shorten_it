package com.urlshortener.service;

import com.urlshortener.entity.ShortUrl;
import com.urlshortener.repository.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Periodically deactivates links that have passed their expiry date. */
@Component
@RequiredArgsConstructor
@Slf4j
public class LinkCleanupScheduler {

    private final ShortUrlRepository shortUrlRepository;
    private final CacheManager cacheManager;

    @Scheduled(fixedDelayString = "PT15M", initialDelayString = "PT1M")
    @Transactional
    public void deactivateExpiredLinks() {
        List<ShortUrl> expired = shortUrlRepository.findExpiredActiveLinks(LocalDateTime.now());
        if (expired.isEmpty()) {
            return;
        }
        expired.forEach(url -> {
            url.setActive(false);
            var cache = cacheManager.getCache("shortUrls");
            if (cache != null) cache.evict(url.getShortCode());
        });
        shortUrlRepository.saveAll(expired);
        log.info("Deactivated {} expired short link(s)", expired.size());
    }
}
