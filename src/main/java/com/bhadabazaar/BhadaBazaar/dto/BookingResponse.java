package com.bhadabazaar.BhadaBazaar.dto;

import com.bhadabazaar.BhadaBazaar.domain.enums.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BookingResponse(
    Long id,
    String customerName,
    String customerPhone,
    LocalDate fromDate,
    LocalDate toDate,
    BigDecimal totalPrice,
    BigDecimal discount,
    BigDecimal advancePaid,
    BigDecimal remainingAmount,
    BookingStatus status,
    List<ItemResponse> items
) {}
