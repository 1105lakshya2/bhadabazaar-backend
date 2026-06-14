package com.bhadabazaar.BhadaBazaar.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountDeleteRequest(
    @NotBlank(message = "Password is required")
    String password
) {}
