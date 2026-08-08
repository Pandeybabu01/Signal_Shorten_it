package com.urlshortener.exception;

/** Thrown when a redirect target requires a password that wasn't supplied or was incorrect. */
public class PasswordRequiredException extends RuntimeException {
    public PasswordRequiredException(String message) {
        super(message);
    }
}
