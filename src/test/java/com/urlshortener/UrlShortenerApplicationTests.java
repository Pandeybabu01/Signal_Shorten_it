package com.urlshortener;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Smoke test: verifies the full Spring context (security, JPA, scheduling, etc.) wires up cleanly. */
@SpringBootTest
@ActiveProfiles("test")
class UrlShortenerApplicationTests {

    @Test
    void contextLoads() {
    }
}
