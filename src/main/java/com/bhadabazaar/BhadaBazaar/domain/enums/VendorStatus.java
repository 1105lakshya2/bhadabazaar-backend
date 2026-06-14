package com.bhadabazaar.BhadaBazaar.domain.enums;

public enum VendorStatus {
    PENDING,
    APPROVED,
    REJECTED,
    SUSPENDED, // set by admin; cannot log in
    DELETED    // user requested deletion; in grace window, hard-purged after it elapses
}
