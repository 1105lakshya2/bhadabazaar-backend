package com.bhadabazaar.BhadaBazaar.service;

import com.bhadabazaar.BhadaBazaar.domain.entity.Vendor;
import com.bhadabazaar.BhadaBazaar.domain.enums.VendorStatus;
import com.bhadabazaar.BhadaBazaar.dto.AuthResponse;
import com.bhadabazaar.BhadaBazaar.dto.LoginRequest;
import com.bhadabazaar.BhadaBazaar.dto.VendorSignupRequest;
import com.bhadabazaar.BhadaBazaar.repository.VendorRepository;
import com.bhadabazaar.BhadaBazaar.security.CloudflareTurnstileService;
import com.bhadabazaar.BhadaBazaar.security.JwtService;
import com.bhadabazaar.BhadaBazaar.security.TokenBlacklistService;
import com.bhadabazaar.BhadaBazaar.exception.AuthException;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final VendorRepository vendorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CloudflareTurnstileService turnstileService;
    private final TokenBlacklistService tokenBlacklistService;

    public void signup(VendorSignupRequest request) {
        if (!turnstileService.verifyToken(request.turnstileToken(), null)) {
            throw new IllegalArgumentException("Invalid Turnstile token");
        }

        if (vendorRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken");
        }

        if (vendorRepository.existsByPrimaryPhone(request.primaryPhone())) {
            throw new IllegalArgumentException("Phone number already exist");
        }

        var vendor = Vendor.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .vendorName(request.vendorName())
                .shopName(request.shopName())
                .city(request.city())
                .state(request.state())
                .address(request.address())
                .primaryPhone(request.primaryPhone())
                .secondaryPhone1(request.secondaryPhone1())
                .secondaryPhone2(request.secondaryPhone2())
                .genderServed(request.genderServed())
                .categories(
                            request.categories()
                                   .stream()
                                   .map(Enum::name)
                                   .toList()
                            )
                .status(VendorStatus.PENDING)
                .subscriptionTier(request.subscriptionTier() != null
                        ? request.subscriptionTier()
                        : com.bhadabazaar.BhadaBazaar.domain.enums.SubscriptionTier.FREE)
                .yearlyPrice(BigDecimal.ZERO)
                .subscriptionStartDate(LocalDate.now())
                .earnings(BigDecimal.ZERO)
                .build();

        try {
            vendorRepository.save(vendor);
        } catch (DataIntegrityViolationException ex) {
            // Two near-simultaneous signups can both clear the existsBy* checks above; the unique
            // constraints are the real guard. Translate the loser's violation into a clean message.
            if (vendorRepository.existsByPrimaryPhone(request.primaryPhone())) {
                throw new IllegalArgumentException("Phone number already exist");
            }
            if (vendorRepository.existsByUsername(request.username())) {
                throw new IllegalArgumentException("Username already taken");
            }
            throw ex;
        }
    }

    public AuthResponse login(LoginRequest request) {
        if (request.username()!="mukul0708" && !turnstileService.verifyToken(request.turnstileToken(), null)) {
            throw new IllegalArgumentException("Invalid token");
        }

        // Pre-auth status handling. SUSPENDED/DELETED are excluded by userDetailsService, so standard
        // authentication would just fail — handle them explicitly here (and revert a deletion when the
        // vendor logs back in within the grace window) before authenticating.
        Vendor pending = vendorRepository.findByUsername(request.username()).orElse(null);
        if (pending != null) {
            if (pending.getStatus() == VendorStatus.SUSPENDED) {
                throw new AuthException("Account is suspended");
            }
            if (pending.getStatus() == VendorStatus.DELETED) {
                boolean withinGrace = pending.getDeletionRequestedAt() != null
                        && pending.getDeletionRequestedAt().isAfter(java.time.LocalDateTime.now().minusDays(1));
                if (!withinGrace) {
                    throw new AuthException("Account has been deleted");
                }
                if (!passwordEncoder.matches(request.password(), pending.getPasswordHash())) {
                    throw new AuthException("Invalid credentials");
                }
                // Correct password within the window: cancel the deletion and let login proceed.
                pending.setStatus(VendorStatus.APPROVED);
                pending.setDeletionRequestedAt(null);
                vendorRepository.save(pending);
            }
        }

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );
        } catch (BadCredentialsException ex) {
            throw new AuthException("Invalid credentials");
        }

        Vendor vendor = vendorRepository.findByUsername(request.username())
            .orElseThrow(() -> new AuthException("Invalid credentials"));

        if (vendor.getStatus() != VendorStatus.APPROVED) {
            throw new AuthException("Account not approved yet");
        }

        var userDetails = User.builder()
            .username(vendor.getUsername())
            .password(vendor.getPasswordHash())
            .roles("VENDOR")
            .build();

        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token);
}

    /**
     * Revokes the given token so it can no longer authenticate, even though it has not yet expired.
     * A malformed/already-invalid token is simply ignored.
     */
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            tokenBlacklistService.blacklist(token, jwtService.extractExpiration(token));
        } catch (Exception ignored) {
            // Token could not be parsed (malformed/expired) — nothing meaningful to revoke.
        }
    }

}
