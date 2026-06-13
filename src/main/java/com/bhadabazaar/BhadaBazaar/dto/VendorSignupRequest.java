package com.bhadabazaar.BhadaBazaar.dto;

import com.bhadabazaar.BhadaBazaar.domain.enums.GenderType;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemCategory;
import com.bhadabazaar.BhadaBazaar.domain.enums.SubscriptionTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record VendorSignupRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 30, message = "Username must be 3-30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "Username may only contain letters, digits, '.' and '_'")
    String username,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
    String password,

    @NotBlank(message = "Vendor name is required")
    @Size(max = 100, message = "Vendor name is too long")
    String vendorName,

    @NotBlank(message = "Shop name is required")
    @Size(max = 100, message = "Shop name is too long")
    String shopName,

    @NotBlank(message = "City is required")
    @Size(max = 85, message = "City is too long")
    String city,

    @NotBlank(message = "State is required")
    @Size(max = 85, message = "State is too long")
    String state,

    @Size(max = 255, message = "Address is too long")
    String address,

    @NotBlank(message = "Primary phone is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Primary phone must be a valid number (E.164 format)")
    String primaryPhone,

    // Optional: allow null or empty, but if present must be a valid number.
    @Pattern(regexp = "^(\\+?[1-9]\\d{7,14})?$", message = "Secondary phone must be a valid number (E.164 format)")
    String secondaryPhone1,

    @Pattern(regexp = "^(\\+?[1-9]\\d{7,14})?$", message = "Secondary phone must be a valid number (E.164 format)")
    String secondaryPhone2,

    @NotNull(message = "Gender served is required")
    GenderType genderServed,

    @NotEmpty(message = "At least one category is required")
    List<ItemCategory> categories,

    // Optional; defaults to FREE when omitted (handled in AuthService).
    SubscriptionTier subscriptionTier,

    @NotBlank(message = "Turnstile token is required")
    String turnstileToken
) {}
