package com.bhadabazaar.BhadaBazaar.domain.enums;

/**
 * Vendor subscription tiers and the maximum number of (non-deleted) items each allows.
 * A negative limit means unlimited.
 */
public enum SubscriptionTier {
    FREE(20),
    BASIC(250),
    PRIMARY(500),
    PREMIUM(-1);

    private final int maxItems;

    SubscriptionTier(int maxItems) {
        this.maxItems = maxItems;
    }

    public int getMaxItems() {
        return maxItems;
    }

    public boolean isUnlimited() {
        return maxItems < 0;
    }
}
