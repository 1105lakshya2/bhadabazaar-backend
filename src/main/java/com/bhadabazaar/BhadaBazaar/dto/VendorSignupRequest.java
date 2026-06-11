package com.bhadabazaar.BhadaBazaar.dto;

import com.bhadabazaar.BhadaBazaar.domain.enums.GenderType;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemCategory;
import java.util.List;

public record VendorSignupRequest(
    String username,
    String password,
    String vendorName,
    String shopName,
    String city,
    String state,
    String address,
    String primaryPhone,
    String secondaryPhone1,
    String secondaryPhone2,
    GenderType genderServed,
    List<ItemCategory> categories,
    String turnstileToken
) {}
