package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import com.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShortCodeGeneratorServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    private ShortCodeGeneratorService service;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        AppProperties.ShortCode shortCode = new AppProperties.ShortCode();
        shortCode.setLength(7);
        shortCode.setStrategy("random");
        shortCode.setAlphabet("23456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ");
        props.setShortCode(shortCode);

        service = new ShortCodeGeneratorService(shortUrlRepository, props);
    }

    @Test
    void generatesCodeOfConfiguredLengthWhenNoCollision() {
        when(shortUrlRepository.existsByShortCode(anyString())).thenReturn(false);
        String code = service.generate();
        assertEquals(7, code.length());
    }

    @Test
    void retriesOnCollisionUntilUniqueCodeFound() {
        when(shortUrlRepository.existsByShortCode(anyString()))
                .thenReturn(true, true, false);
        String code = service.generate();
        assertNotNull(code);
        verify(shortUrlRepository, times(3)).existsByShortCode(anyString());
    }
}
