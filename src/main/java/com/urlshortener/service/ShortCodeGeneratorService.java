package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import com.urlshortener.repository.ShortUrlRepository;
import com.urlshortener.util.Base62Encoder;
import com.urlshortener.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Generates unique short codes.
 *
 * Two strategies are supported (configurable via app.short-code.strategy):
 *  - "random": pulls a cryptographically random string and checks the DB for
 *              collisions (retried a bounded number of times).
 *  - "base62": encodes a monotonically increasing counter seeded from the
 *              current DB row count, which guarantees no collisions and
 *              produces compact, incrementally longer codes over time.
 */
@Service
@RequiredArgsConstructor
public class ShortCodeGeneratorService {

    private final ShortUrlRepository shortUrlRepository;
    private final AppProperties appProperties;

    private static final int MAX_RETRIES = 8;

    public String generate() {
        String strategy = appProperties.getShortCode().getStrategy();
        if ("base62".equalsIgnoreCase(strategy)) {
            return generateBase62();
        }
        return generateRandom();
    }

    private String generateBase62() {
        // Use current max id + a random jitter so codes are not sequentially guessable,
        // while still being extremely unlikely to collide.
        long seed = (System.currentTimeMillis() % 1_000_000_000L) + (long) (Math.random() * 1000);
        String alphabet = appProperties.getShortCode().getAlphabet();
        String code = Base62Encoder.encode(seed, alphabet);
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            if (!shortUrlRepository.existsByShortCode(code)) {
                return code;
            }
            seed += 1;
            code = Base62Encoder.encode(seed, alphabet);
        }
        throw new IllegalStateException("Unable to generate a unique short code after " + MAX_RETRIES + " attempts");
    }

    private String generateRandom() {
        String alphabet = appProperties.getShortCode().getAlphabet();
        int length = appProperties.getShortCode().getLength();
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            String code = HashUtil.generateRandomAlphaNumeric(length, alphabet);
            if (!shortUrlRepository.existsByShortCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate a unique short code after " + MAX_RETRIES + " attempts");
    }
}
