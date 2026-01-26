package com.bhadabazaar.BhadaBazaar.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemCategory;
import java.util.*;
import java.util.stream.Collectors;

@Converter
public class ItemCategoryListConverter implements AttributeConverter<List<ItemCategory>, String> {

    @Override
    public String convertToDatabaseColumn(List<ItemCategory> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}"; // Empty PG array
        }
        
        // Join the enums: "item1,item2"
        String joined = attribute.stream()
                                 .map(Enum::name)
                                 .collect(Collectors.joining(","));
        
        // Wrap in curly braces: "{item1,item2}"
        return "{" + joined + "}";
    }

    @Override
    public List<ItemCategory> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.length() <= 2) { // Handles null or "{}"
            return new ArrayList<>();
        }
        
        // Remove the braces "{" and "}"
        String cleanData = dbData.substring(1, dbData.length() - 1);
        
        if (cleanData.isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(cleanData.split(","))
                     .map(ItemCategory::valueOf)
                     .collect(Collectors.toList());
    }
}

