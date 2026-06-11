package com.bhadabazaar.BhadaBazaar.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Audit row written every time a vendor resets their earnings. Records the earnings amount that was
 * cleared and when, so the financial history is never silently destroyed by a reset.
 */
@Entity
@Table(name = "vendor_earnings_resets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorEarningsReset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    /** The earnings balance that was wiped by this reset. */
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "reset_at", nullable = false)
    private LocalDateTime resetAt;

    @PrePersist
    protected void onCreate() {
        if (this.resetAt == null) {
            this.resetAt = LocalDateTime.now();
        }
    }
}
