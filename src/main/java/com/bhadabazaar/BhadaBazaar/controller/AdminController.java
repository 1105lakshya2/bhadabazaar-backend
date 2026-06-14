package com.bhadabazaar.BhadaBazaar.controller;

import com.bhadabazaar.BhadaBazaar.domain.enums.VendorStatus;
import com.bhadabazaar.BhadaBazaar.dto.AdminBulkStatusResponse;
import com.bhadabazaar.BhadaBazaar.dto.AdminVendorResponse;
import com.bhadabazaar.BhadaBazaar.dto.MessageResponse;
import com.bhadabazaar.BhadaBazaar.dto.SubscriptionUpdateRequest;
import com.bhadabazaar.BhadaBazaar.dto.VendorStatusUpdateRequest;
import com.bhadabazaar.BhadaBazaar.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/vendors")
    public ResponseEntity<List<AdminVendorResponse>> getVendorsByStatus(@RequestParam VendorStatus status) {
        return ResponseEntity.ok(adminService.getVendorsByStatus(status));
    }

    @PatchMapping("/vendors/status")
    public ResponseEntity<AdminBulkStatusResponse> updateVendorStatus(@Valid @RequestBody VendorStatusUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateVendorStatus(request.usernames(), request.status()));
    }

    @PatchMapping("/vendors/{username}/subscription")
    public ResponseEntity<MessageResponse> updateSubscriptionTier(
            @PathVariable String username,
            @Valid @RequestBody SubscriptionUpdateRequest request) {
        adminService.updateSubscriptionTier(username, request.tier());
        return ResponseEntity.ok(new MessageResponse("Subscription tier updated to " + request.tier()));
    }
}
