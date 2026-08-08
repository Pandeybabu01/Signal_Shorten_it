package com.urlshortener.util;

/**
 * Encodes numeric IDs into short, URL-safe Base62-style strings using a
 * custom alphabet with visually ambiguous characters (0, O, 1, I, l) removed,
 * to avoid confusing users who read/type a short code aloud or by hand.
 */
public final class Base62Encoder {

    private Base62Encoder() {
    }

    public static String encode(long number, String alphabet) {
        if (number == 0) {
            return String.valueOf(alphabet.charAt(0));
        }
        int base = alphabet.length();
        StringBuilder sb = new StringBuilder();
        long n = number;
        while (n > 0) {
            int remainder = (int) (n % base);
            sb.append(alphabet.charAt(remainder));
            n /= base;
        }
        return sb.reverse().toString();
    }
}
