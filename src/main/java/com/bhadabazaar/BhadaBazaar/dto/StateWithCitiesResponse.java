package com.bhadabazaar.BhadaBazaar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class StateWithCitiesResponse {
    private String state;
    private List<String> cities;
}
