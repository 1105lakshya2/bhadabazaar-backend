package com.bhadabazaar.BhadaBazaar.exception;

/**
 * Thrown when too many consecutive wrong-password attempts have temporarily locked a vendor out of
 * the password-confirm endpoints (earnings / account deletion). Maps to HTTP 429 so the client
 * treats it like the rate limiter — back off and retry later.
 */
public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}
