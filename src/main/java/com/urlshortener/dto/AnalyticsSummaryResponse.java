package com.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryResponse {
    private String shortCode;
    private long totalClicks;
    private long uniqueVisitors;
    private List<TimeSeriesPoint> clicksOverTime;
    private List<LabeledCount> topReferrers;
    private List<LabeledCount> deviceBreakdown;
    private List<LabeledCount> browserBreakdown;
    private List<LabeledCount> osBreakdown;
    private List<LabeledCount> countryBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesPoint {
        private String date;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabeledCount {
        private String label;
        private long count;
    }
}
