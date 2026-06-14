package com.bhadabazaar.BhadaBazaar.dto;

import com.bhadabazaar.BhadaBazaar.domain.enums.SubscriptionTier;
import jakarta.validation.constraints.NotNull;

public record SubscriptionUpdateRequest(
    @NotNull(message = "Subscription tier is required")
    SubscriptionTier tier
) {}
