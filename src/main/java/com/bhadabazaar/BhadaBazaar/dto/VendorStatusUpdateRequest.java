package com.bhadabazaar.BhadaBazaar.dto;

import com.bhadabazaar.BhadaBazaar.domain.enums.VendorStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record VendorStatusUpdateRequest(
    @NotEmpty(message = "At least one username is required")
    List<String> usernames,

    @NotNull(message = "Status is required")
    VendorStatus status
) {}
