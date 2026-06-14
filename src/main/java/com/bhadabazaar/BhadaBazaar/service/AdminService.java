package com.bhadabazaar.BhadaBazaar.service;

import com.bhadabazaar.BhadaBazaar.domain.entity.Vendor;
import com.bhadabazaar.BhadaBazaar.domain.enums.SubscriptionTier;
import com.bhadabazaar.BhadaBazaar.domain.enums.VendorStatus;
import com.bhadabazaar.BhadaBazaar.dto.AdminBulkStatusResponse;
import com.bhadabazaar.BhadaBazaar.dto.AdminVendorResponse;
import com.bhadabazaar.BhadaBazaar.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final VendorRepository vendorRepository;

    /** Vendors with the given status — username, phone, name and tier. Not paginated. */
    @Transactional(readOnly = true)
    public List<AdminVendorResponse> getVendorsByStatus(VendorStatus status) {
        return vendorRepository.findByStatus(status).stream()
                .map(v -> new AdminVendorResponse(
                        v.getUsername(),
                        v.getPrimaryPhone(),
                        v.getVendorName(),
                        v.getSubscriptionTier()))
                .collect(Collectors.toList());
    }

    /**
     * Bulk-sets the status of the given usernames to APPROVED, REJECTED or SUSPENDED. Un-suspending
     * is just setting APPROVED again; suspension takes effect immediately on existing tokens (the
     * userDetailsService gate rejects SUSPENDED). DELETED is not a permitted target here — that is
     * the password-protected user flow + purge — and DELETED vendors are left untouched so an
     * approval can't silently cancel a pending account purge.
     */
    @Transactional
    public AdminBulkStatusResponse updateVendorStatus(List<String> usernames, VendorStatus status) {
        if (status != VendorStatus.APPROVED
                && status != VendorStatus.REJECTED
                && status != VendorStatus.SUSPENDED) {
            throw new IllegalArgumentException("Status must be APPROVED, REJECTED or SUSPENDED");
        }
        List<String> updated = new ArrayList<>();
        List<String> notFound = new ArrayList<>();
        for (String username : usernames) {
            Vendor vendor = vendorRepository.findByUsername(username).orElse(null);
            if (vendor == null || vendor.getStatus() == VendorStatus.DELETED) {
                notFound.add(username);
                continue;
            }
            vendor.setStatus(status);
            vendorRepository.save(vendor);
            updated.add(username);
        }
        return new AdminBulkStatusResponse(updated, notFound);
    }

    /** Sets a vendor's subscription tier. */
    @Transactional
    public void updateSubscriptionTier(String username, SubscriptionTier tier) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        vendor.setSubscriptionTier(tier);
        vendorRepository.save(vendor);
    }
}
