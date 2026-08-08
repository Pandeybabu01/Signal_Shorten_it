package com.urlshortener.controller;

import com.urlshortener.config.AppProperties;
import com.urlshortener.dto.*;
import com.urlshortener.entity.ShortUrl;
import com.urlshortener.entity.User;
import com.urlshortener.exception.RateLimitExceededException;
import com.urlshortener.repository.ShortUrlRepository;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.security.UserPrincipal;
import com.urlshortener.service.QrCodeService;
import com.urlshortener.service.RateLimiterService;
import com.urlshortener.service.UrlShortenerService;
import com.urlshortener.util.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
@Tag(name = "URLs", description = "Create and manage short links")
public class UrlController {

    private final UrlShortenerService urlShortenerService;
    private final QrCodeService qrCodeService;
    private final RateLimiterService rateLimiterService;
    private final UserRepository userRepository;
    private final ShortUrlRepository shortUrlRepository;
    private final AppProperties appProperties;

    @PostMapping
    @Operation(summary = "Shorten a URL",
            description = "Works anonymously (rate-limited by IP) or authenticated (rate-limited by account, links saved to your dashboard)")
    public ResponseEntity<UrlResponse> create(@Valid @RequestBody CreateUrlRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal,
                                               HttpServletRequest httpRequest) {
        String rateLimitKey = principal != null
                ? "user:" + principal.getId()
                : "ip:" + ClientIpResolver.resolve(httpRequest);

        if (!rateLimiterService.tryConsumeForCreation(rateLimitKey)) {
            throw new RateLimitExceededException("Too many links created recently. Please slow down and try again shortly.");
        }

        User owner = principal != null ? userRepository.findById(principal.getId()).orElse(null) : null;
        UrlResponse response = urlShortenerService.createShortUrl(request, owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List my short links (paginated)")
    public ResponseEntity<PagedResponse<UrlResponse>> listMine(@AuthenticationPrincipal UserPrincipal principal,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size) {
        User owner = userRepository.findById(principal.getId()).orElseThrow();
        Page<ShortUrl> result = shortUrlRepository.findByOwner(owner,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        Page<UrlResponse> mapped = result.map(urlShortenerService::toResponse);
        return ResponseEntity.ok(PagedResponse.from(mapped));
    }

    @GetMapping("/{shortCode}")
    @Operation(summary = "Get details for one of my short links")
    public ResponseEntity<UrlResponse> get(@PathVariable String shortCode,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        User owner = userRepository.findById(principal.getId()).orElseThrow();
        return ResponseEntity.ok(urlShortenerService.getForOwner(shortCode, owner));
    }

    @PatchMapping("/{shortCode}")
    @Operation(summary = "Update a short link (title, expiry, active state, password, click limit)")
    public ResponseEntity<UrlResponse> update(@PathVariable String shortCode,
                                               @Valid @RequestBody UpdateUrlRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        User owner = userRepository.findById(principal.getId()).orElseThrow();
        return ResponseEntity.ok(urlShortenerService.update(shortCode, request, owner));
    }

    @DeleteMapping("/{shortCode}")
    @Operation(summary = "Delete a short link")
    public ResponseEntity<Void> delete(@PathVariable String shortCode,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        User owner = userRepository.findById(principal.getId()).orElseThrow();
        urlShortenerService.delete(shortCode, owner);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{shortCode}/qrcode")
    @Operation(summary = "Get a PNG QR code that points to the short link", description = "Public endpoint - no auth required")
    public ResponseEntity<byte[]> qrCode(@PathVariable String shortCode) {
        ShortUrl url = urlShortenerService.findActiveByShortCode(shortCode);
        byte[] png = qrCodeService.generatePng(appProperties.getBaseUrl() + "/r/" + url.getShortCode());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(png);
    }
}
