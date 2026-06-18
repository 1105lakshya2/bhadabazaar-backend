package com.bhadabazaar.BhadaBazaar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BookingCreateRequest(
    @NotBlank(message = "Customer name is required")
    @Size(max = 100, message = "Customer name is too long")
    String customerName,

    @NotBlank(message = "Customer phone is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Customer phone must be a valid number (E.164 format)")
    String customerPhone,

    @NotNull(message = "From date is required")
    LocalDate fromDate,

    @NotNull(message = "To date is required")
    LocalDate toDate,

    // Ignored by the server (the total is computed from item prices); kept for payload compatibility.
    BigDecimal totalAmount,

    @PositiveOrZero(message = "Discount must not be negative")
    BigDecimal discount,

    @PositiveOrZero(message = "Advance paid must not be negative")
    BigDecimal advancePaid,

    @NotEmpty(message = "At least one item is required")
    List<Long> itemIds
) {}
