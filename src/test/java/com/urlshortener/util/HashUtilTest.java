package com.urlshortener.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HashUtilTest {

    @Test
    void sameInputAndPepperProduceSameHash() {
        String h1 = HashUtil.sha256("192.168.1.1", "pepper");
        String h2 = HashUtil.sha256("192.168.1.1", "pepper");
        assertEquals(h1, h2);
    }

    @Test
    void differentPepperProducesDifferentHash() {
        String h1 = HashUtil.sha256("192.168.1.1", "pepperA");
        String h2 = HashUtil.sha256("192.168.1.1", "pepperB");
        assertNotEquals(h1, h2);
    }

    @Test
    void hashNeverContainsRawIp() {
        String ip = "203.0.113.42";
        String hash = HashUtil.sha256(ip, "secret");
        assertFalse(hash.contains(ip));
    }

    @Test
    void apiKeysAreUnique() {
        String k1 = HashUtil.generateApiKey();
        String k2 = HashUtil.generateApiKey();
        assertNotEquals(k1, k2);
        assertTrue(k1.length() > 20);
    }
}
