package com.bhadabazaar.BhadaBazaar.dto;

import com.bhadabazaar.BhadaBazaar.domain.enums.ItemCategory;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemGenderType;
import java.math.BigDecimal;

public record ItemCreateRequest(
    String itemCode,
    String name,
    ItemCategory category,
    ItemGenderType gender,
    BigDecimal pricePerDay,
    Boolean isActive
) {
    public ItemCreateRequest {
        if (isActive == null) {
            isActive = true;
        }
    }
}
