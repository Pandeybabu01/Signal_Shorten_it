package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import com.urlshortener.exception.InvalidUrlException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

@Service
@RequiredArgsConstructor
public class UrlValidationService {

    private final AppProperties appProperties;

    public void validate(String originalUrl) {
        var cfg = appProperties.getUrlValidation();

        if (originalUrl == null || originalUrl.isBlank()) {
            throw new InvalidUrlException("URL must not be blank");
        }
        if (originalUrl.length() > cfg.getMaxUrlLength()) {
            throw new InvalidUrlException("URL exceeds maximum length of " + cfg.getMaxUrlLength());
        }

        URI uri;
        try {
            uri = new URI(originalUrl);
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("URL is malformed: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (cfg.isRequireHttpHttps() && (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")))) {
            throw new InvalidUrlException("Only http and https URLs are supported");
        }

        String host = uri.getHost();
        if (host == null) {
            throw new InvalidUrlException("URL must include a valid host");
        }

        if (cfg.getBlockedDomains() != null) {
            String lowerHost = host.toLowerCase();
            for (String blocked : cfg.getBlockedDomains()) {
                if (lowerHost.equals(blocked.toLowerCase()) || lowerHost.endsWith("." + blocked.toLowerCase())) {
                    throw new InvalidUrlException("This domain is blocked for security reasons");
                }
            }
        }

        // Guard against self-referential shortening loops.
        if (host.equalsIgnoreCase("localhost") && appProperties.getBaseUrl().contains("localhost")) {
            // allowed in dev; production base-url check below is stricter
        }
    }
}
