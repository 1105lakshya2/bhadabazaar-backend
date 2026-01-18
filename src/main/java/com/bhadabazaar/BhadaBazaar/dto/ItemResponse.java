package com.bhadabazaar.BhadaBazaar.dto;

import com.bhadabazaar.BhadaBazaar.domain.enums.ItemCategory;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemGenderType;
import java.math.BigDecimal;
import java.util.List;

public record ItemResponse(
    Long id,
    Long vendorId,
    String itemCode,
    String name,
    ItemCategory category,
    ItemGenderType gender,
    BigDecimal pricePerDay,
    Boolean isActive,
    List<ItemImageResponse> images
) {}
