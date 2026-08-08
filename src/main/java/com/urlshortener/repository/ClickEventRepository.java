package com.urlshortener.repository;

import com.urlshortener.entity.ClickEvent;
import com.urlshortener.entity.ShortUrl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    Page<ClickEvent> findByShortUrlOrderByClickedAtDesc(ShortUrl shortUrl, Pageable pageable);

    long countByShortUrl(ShortUrl shortUrl);

    @Query("SELECT COUNT(DISTINCT c.ipHash) FROM ClickEvent c WHERE c.shortUrl = :shortUrl")
    long countDistinctVisitors(@Param("shortUrl") ShortUrl shortUrl);

    @Query("SELECT FUNCTION('DATE', c.clickedAt) AS day, COUNT(c) AS total " +
           "FROM ClickEvent c WHERE c.shortUrl = :shortUrl AND c.clickedAt >= :since " +
           "GROUP BY FUNCTION('DATE', c.clickedAt) ORDER BY day ASC")
    List<Object[]> countClicksPerDay(@Param("shortUrl") ShortUrl shortUrl, @Param("since") LocalDateTime since);

    @Query("SELECT c.deviceType AS label, COUNT(c) AS total FROM ClickEvent c " +
           "WHERE c.shortUrl = :shortUrl GROUP BY c.deviceType ORDER BY total DESC")
    List<Object[]> countByDeviceType(@Param("shortUrl") ShortUrl shortUrl);

    @Query("SELECT c.browser AS label, COUNT(c) AS total FROM ClickEvent c " +
           "WHERE c.shortUrl = :shortUrl GROUP BY c.browser ORDER BY total DESC")
    List<Object[]> countByBrowser(@Param("shortUrl") ShortUrl shortUrl);

    @Query("SELECT c.os AS label, COUNT(c) AS total FROM ClickEvent c " +
           "WHERE c.shortUrl = :shortUrl GROUP BY c.os ORDER BY total DESC")
    List<Object[]> countByOs(@Param("shortUrl") ShortUrl shortUrl);

    @Query("SELECT c.countryCode AS label, COUNT(c) AS total FROM ClickEvent c " +
           "WHERE c.shortUrl = :shortUrl AND c.countryCode IS NOT NULL GROUP BY c.countryCode ORDER BY total DESC")
    List<Object[]> countByCountry(@Param("shortUrl") ShortUrl shortUrl);

    @Query("SELECT CASE WHEN c.referrer IS NULL OR c.referrer = '' THEN 'Direct' ELSE c.referrer END AS label, " +
           "COUNT(c) AS total FROM ClickEvent c WHERE c.shortUrl = :shortUrl " +
           "GROUP BY label ORDER BY total DESC")
    List<Object[]> countByReferrer(@Param("shortUrl") ShortUrl shortUrl);
}
