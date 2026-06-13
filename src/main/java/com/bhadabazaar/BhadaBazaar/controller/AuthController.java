package com.bhadabazaar.BhadaBazaar.controller;

import com.bhadabazaar.BhadaBazaar.dto.AuthResponse;
import com.bhadabazaar.BhadaBazaar.dto.MessageResponse;
import com.bhadabazaar.BhadaBazaar.dto.LoginRequest;
import com.bhadabazaar.BhadaBazaar.dto.VendorSignupRequest;
import com.bhadabazaar.BhadaBazaar.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signup(@Valid @RequestBody VendorSignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok(new MessageResponse("Registration successful. You will be contacted for verification."));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }
}