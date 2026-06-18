package com.bhadabazaar.BhadaBazaar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username is too long")
    String username,

    // Only presence/length is checked on login; the real charset rules are enforced at signup,
    // and login must accept whatever was already registered.
    @NotBlank(message = "Password is required")
    @Size(max = 100, message = "Password is too long")
    String password,

    String turnstileToken
) {}
