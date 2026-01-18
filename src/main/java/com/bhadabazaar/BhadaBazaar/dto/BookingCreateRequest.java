package com.bhadabazaar.BhadaBazaar.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BookingCreateRequest(
    String customerName,
    String customerPhone,
    LocalDate fromDate,
    LocalDate toDate,
    BigDecimal totalAmount,
    BigDecimal discount,
    BigDecimal advancePaid,
    List<Long> itemIds
) {}
