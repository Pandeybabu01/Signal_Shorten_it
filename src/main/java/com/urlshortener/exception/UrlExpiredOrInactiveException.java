package com.urlshortener.exception;

public class UrlExpiredOrInactiveException extends RuntimeException {
    public UrlExpiredOrInactiveException(String message) {
        super(message);
    }
}
