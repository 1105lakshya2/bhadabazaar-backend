package com.bhadabazaar.BhadaBazaar.service;

import com.bhadabazaar.BhadaBazaar.domain.entity.Vendor;
import com.bhadabazaar.BhadaBazaar.domain.enums.VendorStatus;
import com.bhadabazaar.BhadaBazaar.dto.AuthResponse;
import com.bhadabazaar.BhadaBazaar.dto.LoginRequest;
import com.bhadabazaar.BhadaBazaar.dto.VendorSignupRequest;
import com.bhadabazaar.BhadaBazaar.repository.VendorRepository;
import com.bhadabazaar.BhadaBazaar.security.CloudflareTurnstileService;
import com.bhadabazaar.BhadaBazaar.security.JwtService;
import com.bhadabazaar.BhadaBazaar.exception.AuthException;

import lombok.RequiredArgsConstructor;
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
                .address(request.address())
                .primaryPhone(request.primaryPhone())
                .secondaryPhone1(request.secondaryPhone1())
                .secondaryPhone2(request.secondaryPhone2())
                .genderServed(request.genderServed())
                .categories(request.categories())
                .status(VendorStatus.PENDING)
                .yearlyPrice(BigDecimal.ZERO)
                .subscriptionStartDate(LocalDate.now())
                .build();
        
        vendorRepository.save(vendor);
    }

    public AuthResponse login(LoginRequest request) {
        if (!turnstileService.verifyToken(request.turnstileToken(), null)) {
            throw new IllegalArgumentException("Invalid token");
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

}
