package com.bhadabazaar.BhadaBazaar.controller;

import com.bhadabazaar.BhadaBazaar.domain.enums.ItemCategory;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemGenderType;
import com.bhadabazaar.BhadaBazaar.dto.ItemResponse;
import com.bhadabazaar.BhadaBazaar.dto.PublicVendorResponse;
import com.bhadabazaar.BhadaBazaar.dto.StoreSearchResponse;
import com.bhadabazaar.BhadaBazaar.service.ItemService;
import com.bhadabazaar.BhadaBazaar.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PublicController {

    private final VendorService vendorService;
    private final ItemService itemService;

    @GetMapping("/cities")
    public ResponseEntity<List<String>> getCities() {
        return ResponseEntity.ok(vendorService.getAllCities());
    }

    @GetMapping("/stores/search")
    public ResponseEntity<List<StoreSearchResponse>> searchStores(
            @RequestParam("q") String query
    ) {
        return ResponseEntity.ok(vendorService.searchStores(query));
    }

    @GetMapping("/cities/{cityId}/stores")
    public ResponseEntity<List<PublicVendorResponse>> getStoresByCity(
            @PathVariable("cityId") String city
    ) {
        return ResponseEntity.ok(vendorService.getStoresByCity(city));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<ItemCategory>> getCategories() {
        return ResponseEntity.ok(Arrays.asList(ItemCategory.values()));
    }

    @GetMapping("/stores/{storeId}/items")
    public ResponseEntity<Page<ItemResponse>> getStoreItems(
            @PathVariable("storeId") Long storeId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(itemService.getAvailableItems(storeId, from, to, category, gender, search, PageRequest.of(page, pageSize)));
    }

    @GetMapping("/items/{itemId}")
    public ResponseEntity<ItemResponse> getItemDetails(@PathVariable("itemId") Long itemId) {
        return ResponseEntity.ok(itemService.getItemDetails(itemId));
    }
}
