package com.bhadabazaar.BhadaBazaar.service;

import com.bhadabazaar.BhadaBazaar.domain.entity.Vendor;
import com.bhadabazaar.BhadaBazaar.domain.enums.BookingStatus;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemCategory;
import com.bhadabazaar.BhadaBazaar.domain.enums.VendorStatus;
import com.bhadabazaar.BhadaBazaar.dto.PublicVendorResponse;
import com.bhadabazaar.BhadaBazaar.dto.StoreSearchResponse;
import com.bhadabazaar.BhadaBazaar.dto.VendorDashboardStats;
import com.bhadabazaar.BhadaBazaar.dto.VendorResponse;
import com.bhadabazaar.BhadaBazaar.repository.BookingRepository;
import com.bhadabazaar.BhadaBazaar.repository.ItemRepository;
import com.bhadabazaar.BhadaBazaar.repository.VendorRepository;
import com.bhadabazaar.BhadaBazaar.security.CloudflareTurnstileService;
import org.springframework.web.multipart.MultipartFile;
import com.bhadabazaar.BhadaBazaar.service.CloudflareImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final CloudflareImageService cloudinaryService;
    private final CloudflareTurnstileService turnstileService;
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;

    public VendorResponse getVendorProfile(String username) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        return mapToResponse(vendor);
    }

    public VendorResponse getVendorProfile(Long storeId) {
        Vendor vendor = vendorRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        return mapToResponse(vendor);
    }

    public List<ItemCategory> getVendorCategories(String username) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        return Arrays.stream(vendor.getCategories())
            .map(ItemCategory::valueOf)
            .toList();
    }

        public java.math.BigDecimal getEarnings(String username) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        return vendor.getEarnings();
    }

    public void resetEarnings(String username) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        vendor.setEarnings(java.math.BigDecimal.ZERO);
        vendorRepository.save(vendor);
    }

    public VendorDashboardStats getVendorStats(String username) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        Long vendorId = vendor.getId();
        
        long bookings = bookingRepository.countByVendorIdAndStatus(vendorId, BookingStatus.BOOKED);
        long returns = bookingRepository.countByVendorIdAndStatus(vendorId, BookingStatus.RETURN_PENDING);
        long items = itemRepository.countByVendorIdAndIsDeletedFalse(vendorId);
        
        return new VendorDashboardStats(bookings, returns, items);
    }

    public List<String> getAllCities() {
        return vendorRepository.findAllCities();
    }

    public List<StoreSearchResponse> searchStores(String query) {
        return vendorRepository.searchVendorsList(query).stream()
                .map(v -> new StoreSearchResponse(
                        v.getId(),
                        v.getCity(),
                        v.getShopName()
                ))
                .collect(Collectors.toList());
    }

    public List<PublicVendorResponse> getStoresByCity(String city) {
        return vendorRepository.findByCityAndStatus(city, VendorStatus.APPROVED).stream()
                .map(this::mapToPublicResponse)
                .collect(Collectors.toList());
    }

    public VendorResponse updateStoreImage(String username, MultipartFile file) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        
        if (vendor.getStoreImagePublicId() != null) {
            cloudinaryService.deleteFile(vendor.getStoreImagePublicId());
        }
        
        java.util.Map<String, String> upload = cloudinaryService.uploadFile(file);
        vendor.setStoreImageUrl(upload.get("url"));
        vendor.setStoreImagePublicId(upload.get("public_id"));
        vendorRepository.save(vendor);
        return mapToResponse(vendor);
    }

    private List<ItemCategory> mapCategories(Vendor vendor) {
    return Arrays.stream(vendor.getCategories())
            .map(ItemCategory::valueOf)
            .toList();
    }

    private PublicVendorResponse mapToPublicResponse(Vendor vendor) {
        return new PublicVendorResponse(
                vendor.getId(),
                vendor.getUsername(),
                vendor.getVendorName(),
                vendor.getShopName(),
                vendor.getCity(),
                vendor.getAddress(),
                vendor.getPrimaryPhone(),
                vendor.getSecondaryPhone1(),
                vendor.getSecondaryPhone2(),
                vendor.getGenderServed(),
                mapCategories(vendor),
                vendor.getStoreImageUrl()
        );
    }

    private VendorResponse mapToResponse(Vendor vendor) {
        return new VendorResponse(
                vendor.getId(),
                vendor.getUsername(),
                vendor.getVendorName(),
                vendor.getShopName(),
                vendor.getCity(),
                vendor.getAddress(),
                vendor.getPrimaryPhone(),
                vendor.getSecondaryPhone1(),
                vendor.getSecondaryPhone2(),
                vendor.getGenderServed(),
                mapCategories(vendor),
                vendor.getStoreImageUrl(),
                vendor.getStatus(),
                vendor.getYearlyPrice(),
                vendor.getSubscriptionStartDate()
        );
    }
}
