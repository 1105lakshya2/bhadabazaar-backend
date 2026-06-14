package com.bhadabazaar.BhadaBazaar.exception;

/**
 * Thrown for expected, user-facing failures whose message is safe to return to the client
 * (e.g. "Item not found", "Image does not belong to item"). The global handler echoes this
 * message back, unlike unexpected exceptions which are masked behind a generic response so
 * internal/database details never reach the API.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
