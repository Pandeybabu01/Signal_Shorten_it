package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import com.urlshortener.exception.InvalidUrlException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UrlValidationServiceTest {

    private UrlValidationService service;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.setBaseUrl("http://localhost:8080");
        AppProperties.UrlValidation validation = new AppProperties.UrlValidation();
        validation.setMaxUrlLength(2048);
        validation.setRequireHttpHttps(true);
        validation.setBlockedDomains(List.of("malware-test.example"));
        props.setUrlValidation(validation);

        service = new UrlValidationService(props);
    }

    @Test
    void acceptsValidHttpsUrl() {
        assertDoesNotThrow(() -> service.validate("https://example.com/page"));
    }

    @Test
    void rejectsBlankUrl() {
        assertThrows(InvalidUrlException.class, () -> service.validate(" "));
    }

    @Test
    void rejectsNonHttpScheme() {
        assertThrows(InvalidUrlException.class, () -> service.validate("ftp://example.com/file"));
    }

    @Test
    void rejectsMalformedUrl() {
        assertThrows(InvalidUrlException.class, () -> service.validate("not a url"));
    }

    @Test
    void rejectsBlockedDomain() {
        assertThrows(InvalidUrlException.class, () -> service.validate("https://malware-test.example/bad"));
    }

    @Test
    void rejectsBlockedSubdomain() {
        assertThrows(InvalidUrlException.class, () -> service.validate("https://sub.malware-test.example/bad"));
    }

    @Test
    void rejectsUrlExceedingMaxLength() {
        String longPath = "a".repeat(3000);
        assertThrows(InvalidUrlException.class, () -> service.validate("https://example.com/" + longPath));
    }
}
