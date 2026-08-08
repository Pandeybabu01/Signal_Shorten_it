package com.urlshortener.service;

import com.urlshortener.dto.AnalyticsSummaryResponse;
import com.urlshortener.dto.AnalyticsSummaryResponse.LabeledCount;
import com.urlshortener.dto.AnalyticsSummaryResponse.TimeSeriesPoint;
import com.urlshortener.entity.ShortUrl;
import com.urlshortener.entity.User;
import com.urlshortener.exception.UnauthorizedAccessException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse summarize(String shortCode, User requester, int daysBack) {
        ShortUrl url = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (requester == null || (url.getOwner() != null && !url.getOwner().getId().equals(requester.getId())
                && requester.getRole() != User.Role.ADMIN)) {
            throw new UnauthorizedAccessException("You do not have permission to view analytics for this link");
        }

        long totalClicks = clickEventRepository.countByShortUrl(url);
        long uniqueVisitors = clickEventRepository.countDistinctVisitors(url);

        LocalDateTime since = LocalDateTime.now().minusDays(daysBack);

        List<TimeSeriesPoint> series = clickEventRepository.countClicksPerDay(url, since).stream()
                .map(row -> TimeSeriesPoint.builder()
                        .date(row[0] instanceof Date d ? d.toString() : String.valueOf(row[0]))
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();

        return AnalyticsSummaryResponse.builder()
                .shortCode(shortCode)
                .totalClicks(totalClicks)
                .uniqueVisitors(uniqueVisitors)
                .clicksOverTime(series)
                .topReferrers(toLabeledCounts(clickEventRepository.countByReferrer(url)))
                .deviceBreakdown(toLabeledCounts(clickEventRepository.countByDeviceType(url)))
                .browserBreakdown(toLabeledCounts(clickEventRepository.countByBrowser(url)))
                .osBreakdown(toLabeledCounts(clickEventRepository.countByOs(url)))
                .countryBreakdown(toLabeledCounts(clickEventRepository.countByCountry(url)))
                .build();
    }

    private List<LabeledCount> toLabeledCounts(List<Object[]> rows) {
        return rows.stream()
                .map(row -> LabeledCount.builder()
                        .label(row[0] != null ? row[0].toString() : "Unknown")
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();
    }
}
