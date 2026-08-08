package com.urlshortener.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String baseUrl;
    private ShortCode shortCode = new ShortCode();
    private RateLimit rateLimit = new RateLimit();
    private UrlValidation urlValidation = new UrlValidation();
    private QrCode qrCode = new QrCode();

    @Data
    public static class ShortCode {
        private int length = 7;
        private String strategy = "base62";
        private String alphabet;
    }

    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private Bucket creation = new Bucket();
        private Bucket redirect = new Bucket();

        @Data
        public static class Bucket {
            private int capacity;
            private int refillTokens;
            private int refillDurationSeconds;
        }
    }

    @Data
    public static class UrlValidation {
        private List<String> blockedDomains;
        private int maxUrlLength = 2048;
        private boolean requireHttpHttps = true;
    }

    @Data
    public static class QrCode {
        private int sizePx = 300;
    }
}
