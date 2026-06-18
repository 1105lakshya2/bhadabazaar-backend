package com.bhadabazaar.BhadaBazaar.service;

import com.bhadabazaar.BhadaBazaar.domain.entity.Vendor;
import com.bhadabazaar.BhadaBazaar.domain.entity.VendorEarningsReset;
import com.bhadabazaar.BhadaBazaar.domain.enums.BookingStatus;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemCategory;
import com.bhadabazaar.BhadaBazaar.domain.enums.VendorStatus;
import com.bhadabazaar.BhadaBazaar.dto.EarningsResetResponse;
import com.bhadabazaar.BhadaBazaar.dto.PublicVendorResponse;
import com.bhadabazaar.BhadaBazaar.dto.StoreSearchResponse;
import com.bhadabazaar.BhadaBazaar.dto.VendorDashboardStats;
import com.bhadabazaar.BhadaBazaar.dto.VendorResponse;
import com.bhadabazaar.BhadaBazaar.repository.BookingItemRepository;
import com.bhadabazaar.BhadaBazaar.repository.BookingRepository;
import com.bhadabazaar.BhadaBazaar.repository.ItemImageRepository;
import com.bhadabazaar.BhadaBazaar.repository.ItemRepository;
import com.bhadabazaar.BhadaBazaar.repository.VendorEarningsResetRepository;
import com.bhadabazaar.BhadaBazaar.repository.VendorRepository;
import com.bhadabazaar.BhadaBazaar.security.CloudflareTurnstileService;
import com.bhadabazaar.BhadaBazaar.exception.AuthException;
import com.bhadabazaar.BhadaBazaar.exception.BusinessException;
import com.bhadabazaar.BhadaBazaar.exception.AccountLockedException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.web.multipart.MultipartFile;
import com.bhadabazaar.BhadaBazaar.service.CloudflareImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final CloudflareImageService cloudinaryService;
    private final CloudflareTurnstileService turnstileService;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;
    private final VendorEarningsResetRepository vendorEarningsResetRepository;
    private final PasswordEncoder passwordEncoder;

    // After this many consecutive wrong-password attempts on the password-confirm endpoints, the
    // vendor is locked out for LOCKOUT_WINDOW. This defends against slow brute force that stays
    // under the 5/min rate-limit bucket. Counter is keyed by username, cleared on a correct password.
    private static final int MAX_PASSWORD_ATTEMPTS = 5;
    private static final Duration LOCKOUT_WINDOW = Duration.ofMinutes(15);

    private final Cache<String, Integer> failedPasswordAttempts = Caffeine.newBuilder()
            // expireAfterWrite, not access: the window starts at the last failed attempt and the lock
            // lifts after LOCKOUT_WINDOW of quiet, regardless of how often the client polls.
            .expireAfterWrite(LOCKOUT_WINDOW)
            .maximumSize(50_000)
            .build();

    public VendorResponse getVendorProfile(String username) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Vendor not found"));
        return mapToResponse(vendor);
    }

    public VendorResponse getVendorProfile(Long storeId) {
        Vendor vendor = vendorRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException("Vendor not found"));
        // Hide suspended and pending-deletion stores from the public store page.
        if (vendor.getStatus() == VendorStatus.SUSPENDED || vendor.getStatus() == VendorStatus.DELETED) {
            throw new BusinessException("Vendor not found");
        }
        return mapToResponse(vendor);
    }

    public List<ItemCategory> getVendorCategories(String username) {
    Vendor vendor = vendorRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("Vendor not found"));

    return mapCategories(vendor); // Reuse the helper method to keep it DRY
    }

    /**
     * Verifies the vendor's password before returning the vendor; used by all sensitive password-confirm
     * endpoints (earnings reads/writes and account deletion). Enforces a lockout after
     * {@link #MAX_PASSWORD_ATTEMPTS} consecutive wrong attempts and clears the counter on success.
     */
    private Vendor verifyPassword(String username, String rawPassword) {
        Integer attempts = failedPasswordAttempts.getIfPresent(username);
        if (attempts != null && attempts >= MAX_PASSWORD_ATTEMPTS) {
            // Locked: don't even check the password, so attempts during the lock don't extend it.
            throw new AccountLockedException(
                    "Too many incorrect password attempts. Please try again after "
                            + LOCKOUT_WINDOW.toMinutes() + " minutes.");
        }

        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Vendor not found"));
        if (!passwordEncoder.matches(rawPassword, vendor.getPasswordHash())) {
            // merge is atomic on the backing ConcurrentMap, so concurrent attempts count correctly.
            failedPasswordAttempts.asMap().merge(username, 1, Integer::sum);
            throw new AuthException("Incorrect password");
        }

        failedPasswordAttempts.invalidate(username);
        return vendor;
    }

    public java.math.BigDecimal getEarnings(String username, String rawPassword) {
        Vendor vendor = verifyPassword(username, rawPassword);
        return vendor.getEarnings();
    }

    @Transactional
    public void resetEarnings(String username, String rawPassword) {
        Vendor vendor = verifyPassword(username, rawPassword);

        // Snapshot the balance being cleared so the financial history survives the reset.
        VendorEarningsReset snapshot = VendorEarningsReset.builder()
                .vendor(vendor)
                .amount(vendor.getEarnings())
                .build();
        vendorEarningsResetRepository.save(snapshot);

        vendor.setEarnings(java.math.BigDecimal.ZERO);
        vendorRepository.save(vendor);
    }

    /** Returns the most recent earnings resets for the vendor (newest first), capped at 50. */
    public List<EarningsResetResponse> getEarningsResetHistory(String username, String rawPassword) {
        Vendor vendor = verifyPassword(username, rawPassword);
        return vendorEarningsResetRepository
                .findByVendorIdOrderByResetAtDesc(vendor.getId(), PageRequest.of(0, 50))
                .stream()
                .map(r -> new EarningsResetResponse(r.getId(), r.getAmount(), r.getResetAt()))
                .collect(Collectors.toList());
    }

    /**
     * Requests account deletion. Password-protected: only succeeds with the vendor's correct
     * password. Sets status to DELETED, which immediately disables the account (existing JWTs stop
     * working), and starts a 24h grace window. Logging back in within the window cancels it;
     * otherwise {@link #finalizeExpiredDeletions()} hard-deletes the account.
     */
    @Transactional
    public void requestAccountDeletion(String username, String rawPassword) {
        Vendor vendor = verifyPassword(username, rawPassword);
        vendor.setStatus(VendorStatus.DELETED);
        vendor.setDeletionRequestedAt(LocalDateTime.now());
        vendorRepository.save(vendor);
    }

    /**
     * Hard-deletes accounts whose deletion grace window has elapsed: every trace of the vendor is
     * purged (Cloudflare images, then DB rows for images, booking items, bookings, items, earnings
     * resets, and finally the vendor) so nothing about them is retained. Runs once daily at 03:00.
     */
    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void finalizeExpiredDeletions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        List<Vendor> expired = vendorRepository.findByStatusAndDeletionRequestedAtBefore(
                VendorStatus.DELETED, cutoff);
        for (Vendor vendor : expired) {
            hardDeleteVendor(vendor);
        }
    }

    /** Permanently removes a vendor and all associated data, including remote Cloudflare images. */
    private void hardDeleteVendor(Vendor vendor) {
        Long vendorId = vendor.getId();

        // 1. Remote cleanup: delete every Cloudflare image owned by this vendor.
        for (String publicId : itemImageRepository.findImagePublicIdsByVendorId(vendorId)) {
            cloudinaryService.deleteFile(publicId);
        }
        if (vendor.getStoreImagePublicId() != null) {
            cloudinaryService.deleteFile(vendor.getStoreImagePublicId());
        }

        // 2. DB purge in FK-safe order (children before parents).
        bookingItemRepository.deleteByVendorId(vendorId);
        itemImageRepository.deleteByVendorId(vendorId);
        bookingRepository.deleteByVendorId(vendorId);
        itemRepository.deleteByVendorId(vendorId);
        vendorEarningsResetRepository.deleteByVendorId(vendorId);
        vendorRepository.delete(vendor);
    }

    public VendorDashboardStats getVendorStats(String username) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Vendor not found"));
        Long vendorId = vendor.getId();
        
        long bookings = bookingRepository.countByVendorIdAndStatus(vendorId, BookingStatus.BOOKED);
        long returns = bookingRepository.countByVendorIdAndStatus(vendorId, BookingStatus.RETURN_PENDING);
        long items = itemRepository.countByVendorIdAndIsDeletedFalse(vendorId);
        
        return new VendorDashboardStats(bookings, returns, items);
    }

    public List<String> getAllCities() {
        return vendorRepository.findAllCities();
    }

    public Map<String, List<String>> getStatesCitiesMap() {
        return vendorRepository.findDistinctStateCityPairs().stream()
                .collect(Collectors.groupingBy(
                        row -> (String) row[0],
                        Collectors.mapping(row -> (String) row[1], Collectors.toList())
                ));
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
                .orElseThrow(() -> new BusinessException("Vendor not found"));
        
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
    if (vendor.getCategories() == null) {
        return List.of();
    }
    return vendor.getCategories().stream()
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
