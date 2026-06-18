package com.bhadabazaar.BhadaBazaar.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for sensitive vendor actions that re-verify the account password. */
public record PasswordConfirmRequest(
    @NotBlank(message = "Password is required")
    String password
) {}
