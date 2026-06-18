package com.bhadabazaar.BhadaBazaar.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EarningsResetResponse(
    Long id,
    BigDecimal amount,
    LocalDateTime resetAt
) {}
