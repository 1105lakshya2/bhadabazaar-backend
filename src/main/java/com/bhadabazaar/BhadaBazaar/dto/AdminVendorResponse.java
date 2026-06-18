package com.bhadabazaar.BhadaBazaar.dto;

import com.bhadabazaar.BhadaBazaar.domain.enums.SubscriptionTier;

public record AdminVendorResponse(
    String username,
    String primaryPhone,
    String vendorName,
    SubscriptionTier subscriptionTier
) {}
