package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UpdateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.entity.ShortUrl;
import com.urlshortener.entity.User;
import com.urlshortener.exception.*;
import com.urlshortener.repository.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private final ShortUrlRepository shortUrlRepository;
    private final ShortCodeGeneratorService shortCodeGeneratorService;
    private final UrlValidationService urlValidationService;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Transactional
    public UrlResponse createShortUrl(CreateUrlRequest request, User owner) {
        urlValidationService.validate(request.getOriginalUrl());

        String shortCode;
        boolean isCustom = request.getCustomAlias() != null && !request.getCustomAlias().isBlank();
        if (isCustom) {
            shortCode = request.getCustomAlias();
            if (shortUrlRepository.existsByShortCode(shortCode)) {
                throw new AliasAlreadyExistsException(shortCode);
            }
        } else {
            shortCode = shortCodeGeneratorService.generate();
        }

        ShortUrl entity = ShortUrl.builder()
                .shortCode(shortCode)
                .originalUrl(request.getOriginalUrl())
                .customAlias(isCustom)
                .title(request.getTitle())
                .owner(owner)
                .expiresAt(request.getExpiresAt())
                .maxClicks(request.getMaxClicks())
                .active(true)
                .clickCount(0)
                .build();

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            entity.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        ShortUrl saved = shortUrlRepository.save(entity);
        return toResponse(saved);
    }

    @Cacheable(value = "shortUrls", key = "#shortCode")
    @Transactional(readOnly = true)
    public ShortUrl findActiveByShortCode(String shortCode) {
        ShortUrl url = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
        if (!url.isActive()) {
            throw new UrlExpiredOrInactiveException("This link has been deactivated");
        }
        if (url.isExpired()) {
            throw new UrlExpiredOrInactiveException("This link expired on " + url.getExpiresAt());
        }
        if (url.hasReachedClickLimit()) {
            throw new UrlExpiredOrInactiveException("This link has reached its maximum number of clicks");
        }
        return url;
    }

    /** Validates a password for a protected link. Throws if incorrect or not required-but-supplied wrongly. */
    public void verifyPassword(ShortUrl url, String suppliedPassword) {
        if (!url.isPasswordProtected()) {
            return;
        }
        if (suppliedPassword == null || !passwordEncoder.matches(suppliedPassword, url.getPasswordHash())) {
            throw new PasswordRequiredException("A valid password is required to access this link");
        }
    }

    @Transactional
    @CacheEvict(value = "shortUrls", key = "#result.shortCode")
    public ShortUrl registerClickAndMaybeDeactivate(Long shortUrlId) {
        ShortUrl url = shortUrlRepository.findByIdForUpdate(shortUrlId)
                .orElseThrow(() -> new UrlNotFoundException("id=" + shortUrlId));
        url.setClickCount(url.getClickCount() + 1);
        if (url.hasReachedClickLimit()) {
            url.setActive(false);
        }
        return shortUrlRepository.save(url);
    }

    @Transactional(readOnly = true)
    public UrlResponse getForOwner(String shortCode, User owner) {
        ShortUrl url = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
        assertOwnership(url, owner);
        return toResponse(url);
    }

    @Transactional
    @CacheEvict(value = "shortUrls", key = "#shortCode")
    public UrlResponse update(String shortCode, UpdateUrlRequest request, User owner) {
        ShortUrl url = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
        assertOwnership(url, owner);

        if (request.getTitle() != null) url.setTitle(request.getTitle());
        if (request.getActive() != null) url.setActive(request.getActive());
        if (request.getExpiresAt() != null) url.setExpiresAt(request.getExpiresAt());
        if (request.getMaxClicks() != null) url.setMaxClicks(request.getMaxClicks());
        if (request.getPassword() != null) {
            url.setPasswordHash(request.getPassword().isBlank() ? null : passwordEncoder.encode(request.getPassword()));
        }

        return toResponse(shortUrlRepository.save(url));
    }

    @Transactional
    @CacheEvict(value = "shortUrls", key = "#shortCode")
    public void delete(String shortCode, User owner) {
        ShortUrl url = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
        assertOwnership(url, owner);
        shortUrlRepository.delete(url);
    }

    private void assertOwnership(ShortUrl url, User owner) {
        if (owner == null || url.getOwner() == null || !url.getOwner().getId().equals(owner.getId())) {
            if (owner == null || owner.getRole() != User.Role.ADMIN) {
                throw new UnauthorizedAccessException("You do not have permission to access this link");
            }
        }
    }

    public UrlResponse toResponse(ShortUrl url) {
        return UrlResponse.builder()
                .id(url.getId())
                .shortCode(url.getShortCode())
                .shortUrl(appProperties.getBaseUrl() + "/r/" + url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .title(url.getTitle())
                .customAlias(url.isCustomAlias())
                .passwordProtected(url.isPasswordProtected())
                .clickCount(url.getClickCount())
                .maxClicks(url.getMaxClicks())
                .active(url.isActive())
                .expired(url.isExpired())
                .expiresAt(url.getExpiresAt())
                .createdAt(url.getCreatedAt())
                .updatedAt(url.getUpdatedAt())
                .build();
    }
}
