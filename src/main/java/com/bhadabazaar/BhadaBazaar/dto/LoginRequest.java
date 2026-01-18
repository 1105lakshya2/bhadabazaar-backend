package com.bhadabazaar.BhadaBazaar.dto;

public record LoginRequest(
    String username,
    String password,
    String turnstileToken
) {}
