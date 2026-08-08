package com.urlshortener.util;

import jakarta.servlet.http.HttpServletRequest;

/** Resolves the real client IP, honoring common reverse-proxy headers. */
public final class ClientIpResolver {

    private static final String[] HEADERS = {
            "X-Forwarded-For", "X-Real-IP", "CF-Connecting-IP", "Proxy-Client-IP"
    };

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        for (String header : HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
