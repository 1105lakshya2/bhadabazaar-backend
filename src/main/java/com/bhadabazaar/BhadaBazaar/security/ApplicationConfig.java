package com.bhadabazaar.BhadaBazaar.security;

import com.bhadabazaar.BhadaBazaar.domain.entity.Vendor;
import com.bhadabazaar.BhadaBazaar.domain.enums.VendorStatus;
import com.bhadabazaar.BhadaBazaar.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final VendorRepository vendorRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            Vendor vendor = vendorRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            // SUSPENDED/DELETED accounts must not authenticate — this also invalidates any
            // still-unexpired JWT they hold, since the filter resolves users through here.
            if (vendor.getStatus() == VendorStatus.SUSPENDED
                    || vendor.getStatus() == VendorStatus.DELETED) {
                throw new UsernameNotFoundException("User not available");
            }
            return User.builder()
                    .username(vendor.getUsername())
                    .password(vendor.getPasswordHash())
                    .roles("VENDOR")
                    .build();
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
