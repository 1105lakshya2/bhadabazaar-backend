package com.bhadabazaar.BhadaBazaar.dto;

import java.util.List;

public record AdminBulkStatusResponse(
    List<String> updated,
    List<String> notFound
) {}
