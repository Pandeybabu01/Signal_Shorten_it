package com.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "click_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "short_url_id", nullable = false)
    private ShortUrl shortUrl;

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;

    /** Raw IP, kept only if privacy-anonymization is disabled. Usually null. */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /** Salted hash of IP - used for rough unique-visitor counting without storing PII. */
    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(length = 1024)
    private String referrer;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(length = 120)
    private String city;

    @Column(name = "device_type", length = 20)
    private String deviceType;

    @Column(length = 60)
    private String browser;

    @Column(length = 60)
    private String os;

    @PrePersist
    void onCreate() {
        if (clickedAt == null) {
            clickedAt = LocalDateTime.now();
        }
    }
}
