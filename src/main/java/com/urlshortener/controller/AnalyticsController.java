package com.urlshortener.controller;

import com.urlshortener.dto.AnalyticsSummaryResponse;
import com.urlshortener.entity.User;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.security.UserPrincipal;
import com.urlshortener.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Click analytics per short link, for the dashboard")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    @GetMapping("/{shortCode}")
    @Operation(summary = "Get click analytics for one of my short links",
            description = "Includes totals, a daily time series, and breakdowns by referrer, device, browser, OS and country")
    public ResponseEntity<AnalyticsSummaryResponse> summary(@PathVariable String shortCode,
                                                              @RequestParam(defaultValue = "30") int days,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        User requester = userRepository.findById(principal.getId()).orElseThrow();
        return ResponseEntity.ok(analyticsService.summarize(shortCode, requester, Math.min(days, 365)));
    }
}
