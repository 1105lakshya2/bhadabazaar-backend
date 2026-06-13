package com.bhadabazaar.BhadaBazaar.dto;

import com.bhadabazaar.BhadaBazaar.domain.enums.ItemCategory;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemGenderType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ItemCreateRequest(
    @NotBlank(message = "Item code is required")
    @Size(max = 50, message = "Item code is too long")
    String itemCode,

    @NotBlank(message = "Item name is required")
    @Size(max = 150, message = "Item name is too long")
    String name,

    @NotNull(message = "Category is required")
    ItemCategory category,

    @NotNull(message = "Gender is required")
    ItemGenderType gender,

    @NotNull(message = "Price per day is required")
    @Positive(message = "Price per day must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Price per day has too many digits")
    BigDecimal pricePerDay,

    Boolean isActive
) {
    public ItemCreateRequest {
        if (isActive == null) {
            isActive = true;
        }
    }
}
