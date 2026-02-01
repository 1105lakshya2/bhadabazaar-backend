package com.bhadabazaar.BhadaBazaar.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.stream.Collectors;

@Converter
public class StringArrayConverter implements AttributeConverter<String[], String> {

    @Override
    public String convertToDatabaseColumn(String[] attribute) {
        if (attribute == null || attribute.length == 0) {
            return "{}";
        }
        // Properly formats for Postgres: {"item1","item2"}
        return "{" + Arrays.stream(attribute)
                .map(s -> "\"" + s.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",")) + "}";
    }

    @Override
    public String[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty() || dbData.equals("{}")) {
            return new String[0];
        }
        // Remove the curly braces {}
        String cleanData = dbData.substring(1, dbData.length() - 1);
        if (cleanData.isEmpty()) {
            return new String[0];
        }
        
        // Split by comma and clean up quotes
        return Arrays.stream(cleanData.split(","))
                .map(s -> s.replace("\"", "").trim())
                .toArray(String[]::new); // Fixed syntax error here
    }
}
