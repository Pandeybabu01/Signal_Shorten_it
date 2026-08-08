package com.urlshortener.service;

import com.urlshortener.entity.ClickEvent;
import com.urlshortener.entity.ShortUrl;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua_parser.Client;
import ua_parser.Parser;

/**
 * Records a click event off the critical redirect path.
 *
 * Privacy note: the raw IP address is never persisted. Instead we store a
 * salted SHA-256 hash (ipHash) which is enough to approximate unique
 * visitors without retaining anyone's real IP address - see PRIVACY_POLICY.md.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClickTrackingService {

    private final ClickEventRepository clickEventRepository;
    private final Parser userAgentParser = new Parser();

    @Value("${app.security.jwt.secret}")
    private String pepper; // reuse app secret as a pepper for IP hashing (server-side only, never exposed)

    @Async("analyticsExecutor")
    @Transactional
    public void recordClickAsync(ShortUrl shortUrl, String ipAddress, String userAgent,
                                  String referrer, String countryCode, String city) {
        try {
            Client client = userAgentParser.parse(userAgent);

            ClickEvent event = ClickEvent.builder()
                    .shortUrl(shortUrl)
                    .ipHash(ipAddress != null ? HashUtil.sha256(ipAddress, pepper) : null)
                    .userAgent(truncate(userAgent, 512))
                    .referrer(truncate(referrer, 1024))
                    .countryCode(countryCode)
                    .city(city)
                    .deviceType(resolveDeviceType(client))
                    .browser(client.userAgent != null && client.userAgent.family != null ? client.userAgent.family : "Unknown")
                    .os(client.os != null && client.os.family != null ? client.os.family : "Unknown")
                    .build();

            clickEventRepository.save(event);
        } catch (Exception e) {
            // Analytics must never break the redirect flow - log and move on.
            log.warn("Failed to record click analytics for shortCode={}: {}", shortUrl.getShortCode(), e.getMessage());
        }
    }

    private String resolveDeviceType(Client client) {
        if (client.device != null && client.device.family != null) {
            String family = client.device.family.toLowerCase();
            if (family.contains("iphone") || family.contains("android") && !family.contains("tablet")) {
                return "Mobile";
            }
            if (family.contains("ipad") || family.contains("tablet")) {
                return "Tablet";
            }
        }
        return "Desktop";
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }
}
