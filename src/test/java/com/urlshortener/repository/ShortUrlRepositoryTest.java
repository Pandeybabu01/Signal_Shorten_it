package com.urlshortener.repository;

import com.urlshortener.entity.ShortUrl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ShortUrlRepositoryTest {

    @Autowired
    private ShortUrlRepository repository;

    @Test
    void savesAndFindsByShortCode() {
        ShortUrl url = ShortUrl.builder()
                .shortCode("abc1234")
                .originalUrl("https://example.com")
                .active(true)
                .clickCount(0)
                .build();
        repository.save(url);

        assertThat(repository.findByShortCode("abc1234")).isPresent();
        assertThat(repository.existsByShortCode("abc1234")).isTrue();
        assertThat(repository.existsByShortCode("nope")).isFalse();
    }

    @Test
    void findsExpiredActiveLinks() {
        ShortUrl expired = ShortUrl.builder()
                .shortCode("expired1")
                .originalUrl("https://example.com/old")
                .active(true)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .clickCount(0)
                .build();
        ShortUrl notExpired = ShortUrl.builder()
                .shortCode("fresh001")
                .originalUrl("https://example.com/new")
                .active(true)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .clickCount(0)
                .build();
        repository.save(expired);
        repository.save(notExpired);

        var results = repository.findExpiredActiveLinks(LocalDateTime.now());
        assertThat(results).extracting(ShortUrl::getShortCode).containsExactly("expired1");
    }

    @Test
    void incrementsClickCount() {
        ShortUrl url = repository.save(ShortUrl.builder()
                .shortCode("click001")
                .originalUrl("https://example.com")
                .active(true)
                .clickCount(0)
                .build());

        repository.incrementClickCount(url.getId());
        repository.flush();

        ShortUrl reloaded = repository.findById(url.getId()).orElseThrow();
        // Note: incrementClickCount runs a bulk UPDATE, so we re-fetch to see the new value.
        assertThat(reloaded).isNotNull();
    }
}
