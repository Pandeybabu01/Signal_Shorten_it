package com.urlshortener.controller;

import com.urlshortener.dto.RedirectAccessRequest;
import com.urlshortener.entity.ShortUrl;
import com.urlshortener.exception.PasswordRequiredException;
import com.urlshortener.exception.RateLimitExceededException;
import com.urlshortener.service.ClickTrackingService;
import com.urlshortener.service.RateLimiterService;
import com.urlshortener.service.UrlShortenerService;
import com.urlshortener.util.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Public-facing redirect endpoint. Kept deliberately thin and fast:
 * lookup is served from cache, and click logging is dispatched async so it
 * never adds latency to the 302 response the visitor is waiting on.
 */
@RestController
@RequestMapping("/r")
@RequiredArgsConstructor
@Tag(name = "Redirect", description = "Public link resolution - what a browser hits when someone opens a short link")
public class RedirectController {

    private final UrlShortenerService urlShortenerService;
    private final ClickTrackingService clickTrackingService;
    private final RateLimiterService rateLimiterService;

    @GetMapping("/{shortCode}")
    @Operation(summary = "Resolve a short code and redirect (302) to the original URL")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode,
                                          @RequestParam(required = false) String password,
                                          HttpServletRequest request) {
        String ip = ClientIpResolver.resolve(request);
        if (!rateLimiterService.tryConsumeForRedirect("ip:" + ip)) {
            throw new RateLimitExceededException("Too many requests. Please try again shortly.");
        }

        ShortUrl url = urlShortenerService.findActiveByShortCode(shortCode);

        if (url.isPasswordProtected()) {
            try {
                urlShortenerService.verifyPassword(url, password);
            } catch (PasswordRequiredException ex) {
                // Signal the client to prompt for a password rather than a raw 401 with no context.
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .header("X-Password-Required", "true")
                        .build();
            }
        }

        urlShortenerService.registerClickAndMaybeDeactivate(url.getId());

        clickTrackingService.recordClickAsync(
                url,
                ip,
                request.getHeader("User-Agent"),
                request.getHeader("Referer"),
                request.getHeader("CF-IPCountry"), // set automatically if behind Cloudflare; null otherwise
                null
        );

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url.getOriginalUrl()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    @PostMapping("/{shortCode}/unlock")
    @Operation(summary = "Submit a password for a password-protected link and get redirected on success")
    public ResponseEntity<Void> unlock(@PathVariable String shortCode,
                                        @Valid @RequestBody RedirectAccessRequest body,
                                        HttpServletRequest request) {
        ShortUrl url = urlShortenerService.findActiveByShortCode(shortCode);
        urlShortenerService.verifyPassword(url, body.getPassword()); // throws PasswordRequiredException (401) if wrong

        urlShortenerService.registerClickAndMaybeDeactivate(url.getId());
        clickTrackingService.recordClickAsync(
                url, ClientIpResolver.resolve(request), request.getHeader("User-Agent"),
                request.getHeader("Referer"), request.getHeader("CF-IPCountry"), null);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url.getOriginalUrl()))
                .build();
    }
}
