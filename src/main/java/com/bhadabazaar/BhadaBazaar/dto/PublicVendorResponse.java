package com.bhadabazaar.BhadaBazaar.dto;

import com.bhadabazaar.BhadaBazaar.domain.enums.GenderType;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemCategory;
import java.util.List;

public record PublicVendorResponse(
    Long id,
    String username,
    String vendorName,
    String shopName,
    String city,
    String address,
    String primaryPhone,
    String secondaryPhone1,
    String secondaryPhone2,
    GenderType genderServed,
    List<ItemCategory> categories,
    String storeImageUrl
) {}
