package com.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the URL Shortener Service.
 *
 * Features enabled at bootstrap:
 *  - Caching (Caffeine) for hot short-code lookups on the redirect hot path
 *  - Async execution so click/analytics logging never blocks a redirect response
 *  - Scheduling for periodic cleanup of expired links
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class UrlShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}
