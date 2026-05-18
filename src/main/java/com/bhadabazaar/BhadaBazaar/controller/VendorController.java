package com.bhadabazaar.BhadaBazaar.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.bhadabazaar.BhadaBazaar.domain.enums.BookingStatus;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemCategory;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemGenderType;
import com.bhadabazaar.BhadaBazaar.dto.BookingCreateRequest;
import com.bhadabazaar.BhadaBazaar.dto.BookingResponse;
import com.bhadabazaar.BhadaBazaar.dto.ItemCreateRequest;
import com.bhadabazaar.BhadaBazaar.dto.ItemImageResponse;
import com.bhadabazaar.BhadaBazaar.dto.ItemResponse;
import com.bhadabazaar.BhadaBazaar.dto.MessageResponse;
import com.bhadabazaar.BhadaBazaar.dto.VendorDashboardStats;
import com.bhadabazaar.BhadaBazaar.dto.VendorEarnings;
import com.bhadabazaar.BhadaBazaar.dto.VendorResponse;
import com.bhadabazaar.BhadaBazaar.service.BookingService;
import com.bhadabazaar.BhadaBazaar.service.ItemService;
import com.bhadabazaar.BhadaBazaar.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vendor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorController {

    private final VendorService vendorService;
    private final ItemService itemService;
    private final BookingService bookingService;

    @GetMapping("/store")
    public ResponseEntity<VendorResponse> getStore(Authentication authentication) {
        return ResponseEntity.ok(vendorService.getVendorProfile(authentication.getName()));
    }

    @GetMapping("/stats")
    public ResponseEntity<VendorDashboardStats> getVendorStats(Authentication authentication) {
        return ResponseEntity.ok(vendorService.getVendorStats(authentication.getName()));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<ItemCategory>> getVendorCategories(Authentication authentication) {
        return ResponseEntity.ok(vendorService.getVendorCategories(authentication.getName()));
    }
    
    @GetMapping("/{vendorId}/items/search-by-code")
    public ResponseEntity<ItemResponse> getItemByCode(
            Authentication authentication,
            @PathVariable Long vendorId,
            @RequestParam("itemCode") String itemCode
    ) {
        VendorResponse vendor = vendorService.getVendorProfile(authentication.getName());
        if (!vendor.id().equals(vendorId)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(itemService.getItemByCodeForVendor(vendorId, itemCode));
    }
    
    @PostMapping("/store/image")
    public ResponseEntity<VendorResponse> uploadStoreImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(vendorService.updateStoreImage(authentication.getName(), file));
    }

    @GetMapping("/items")
    public ResponseEntity<Page<ItemResponse>> getVendorItems(
            Authentication authentication,
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(required = false) ItemGenderType gender,
            @RequestParam(required = false) String searchByName,
            @RequestParam(required = false) String searchByID,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
         // Normalize search
    if (searchByName != null) {
        searchByName = searchByName.trim();
        if (searchByName.isEmpty()) {
            searchByName = null;
        }
    }
    if (searchByID != null) {
        searchByID = searchByID.trim();
        if (searchByID.isEmpty()) {
            searchByID = null;
        }
    }
        return ResponseEntity.ok(itemService.getVendorItems(authentication.getName(), category, gender,searchByName, searchByID, PageRequest.of(page, pageSize)));
    }

    @PostMapping("/items")
    public ResponseEntity<ItemResponse> createItem(Authentication authentication, @RequestBody ItemCreateRequest request) {
        return ResponseEntity.ok(itemService.createItem(authentication.getName(), request));
    }

    @GetMapping("/items/{itemId}")
    public ResponseEntity<ItemResponse> getItemDetails(@PathVariable Long itemId) {
        // Should verify ownership, but for now just getting details
        return ResponseEntity.ok(itemService.getItemDetails(itemId));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ItemResponse> updateItem(@PathVariable Long itemId, @RequestBody ItemCreateRequest request) {
        return ResponseEntity.ok(itemService.updateItem(itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<MessageResponse> deleteItem(@PathVariable Long itemId) {
        itemService.deleteItem(itemId);
        return ResponseEntity.ok(new MessageResponse("Item deleted successfully"));
    }

    @PostMapping("/items/{itemId}/images")
    public ResponseEntity<MessageResponse> uploadImages(@PathVariable Long itemId, @RequestParam("files") List<MultipartFile> files) {
        itemService.uploadImages(itemId, files);
        return ResponseEntity.ok(new MessageResponse("Images uploaded successfully"));
    }


    @DeleteMapping("/items/{itemId}/images/{imageId}")
    public ResponseEntity<MessageResponse> deleteImage(@PathVariable Long itemId, @PathVariable Long imageId) {
        itemService.deleteImage(itemId, imageId);
        return ResponseEntity.ok(new MessageResponse("Image deleted successfully"));
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> createBooking(Authentication authentication, @RequestBody BookingCreateRequest request) {
        return ResponseEntity.ok(bookingService.createBooking(authentication.getName(), request));
    }

    @GetMapping("/bookings")
    public ResponseEntity<Page<BookingResponse>> getBookings(
            Authentication authentication,
            @RequestParam(required = true) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        if(status == BookingStatus.BOOKED) {
          Pageable pageable = PageRequest.of(page, pageSize, Sort.by("fromDate").ascending()); 
          return ResponseEntity.ok(bookingService.getVendorBookings(authentication.getName(), status, pageable)); 
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("toDate").ascending());
        return ResponseEntity.ok(bookingService.getVendorBookings(authentication.getName(), status, pageable));
    }

    @GetMapping("/bookings/search-by-date")
    public ResponseEntity<Page<BookingResponse>> searchBookingsByDate(
            Authentication authentication,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("fromDate").ascending());
        return ResponseEntity.ok(bookingService.searchBookingsByDate(authentication.getName(), date, pageable));
    }

    @GetMapping("/bookings/return-pending-before")
    public ResponseEntity<Page<BookingResponse>> searchReturnPendingBeforeDate(
            Authentication authentication,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("toDate").ascending());
        return ResponseEntity.ok(bookingService.searchReturnPendingBeforeDate(authentication.getName(), date, pageable));
    }

    @GetMapping("/bookings/search-by-phone")
    public ResponseEntity<List<BookingResponse>> searchBookingsByPhone(
            Authentication authentication,
            @RequestParam("phone") String phone
    ) {
        return ResponseEntity.ok(bookingService.searchBookingsByPhone(authentication.getName(), phone));
    }

    @PatchMapping("/bookings/{bookingId}/return")
    public ResponseEntity<MessageResponse> markReturnPending(@PathVariable Long bookingId) {
        bookingService.updateBookingStatus(bookingId, BookingStatus.RETURN_PENDING);
        return ResponseEntity.ok(new MessageResponse("Booking marked as return pending"));
    }

    @PatchMapping("/bookings/{bookingId}/close")
    public ResponseEntity<MessageResponse> closeBooking(@PathVariable Long bookingId) {
        bookingService.updateBookingStatus(bookingId, BookingStatus.CLOSED);
        return ResponseEntity.ok(new MessageResponse("Booking closed successfully"));
    }
    
    @GetMapping("/items/availability")
    public ResponseEntity<Page<ItemResponse>> getAvailableItems(
            Authentication authentication,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String searchByName,
            @RequestParam(required = false) String searchByID,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
         // Need vendor ID from auth.
    if (searchByName != null) {
        searchByName = searchByName.trim();
        if (searchByName.isEmpty()) {
            searchByName = null;
        }
    }

    if (searchByID != null) {
        searchByID = searchByID.trim();
        if (searchByID.isEmpty()) {
            searchByID = null;
        }
    }
         VendorResponse vendor = vendorService.getVendorProfile(authentication.getName());
         return ResponseEntity.ok(itemService.getAvailableItems(vendor.id(), from, to, category, gender, searchByName,searchByID, PageRequest.of(page, pageSize)));
    }
    
    @GetMapping("/earnings")
    public ResponseEntity<VendorEarnings> getEarnings(Authentication authentication) {
        BigDecimal totalEarnings = vendorService.getEarnings(authentication.getName());
        return ResponseEntity.ok(new VendorEarnings(totalEarnings));
    }
    
    @PostMapping("/earnings/reset")
    public ResponseEntity<MessageResponse> resetEarnings(Authentication authentication) {
        vendorService.resetEarnings(authentication.getName());
        return ResponseEntity.ok(new MessageResponse("Earnings reset successfully"));
    }
}
