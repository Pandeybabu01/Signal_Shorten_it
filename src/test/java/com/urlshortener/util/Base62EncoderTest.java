package com.urlshortener.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    private static final String ALPHABET = "23456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";

    @Test
    void encodesZeroAsFirstAlphabetChar() {
        assertEquals(String.valueOf(ALPHABET.charAt(0)), Base62Encoder.encode(0, ALPHABET));
    }

    @Test
    void encodingIsDeterministic() {
        String first = Base62Encoder.encode(123456L, ALPHABET);
        String second = Base62Encoder.encode(123456L, ALPHABET);
        assertEquals(first, second);
    }

    @Test
    void differentNumbersProduceDifferentCodes() {
        String a = Base62Encoder.encode(1000L, ALPHABET);
        String b = Base62Encoder.encode(1001L, ALPHABET);
        assertNotEquals(a, b);
    }

    @Test
    void onlyUsesCharactersFromAlphabet() {
        String code = Base62Encoder.encode(9999999L, ALPHABET);
        for (char c : code.toCharArray()) {
            assertTrue(ALPHABET.indexOf(c) >= 0, "Unexpected character: " + c);
        }
    }
}
