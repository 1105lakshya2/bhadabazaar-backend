package com.bhadabazaar.BhadaBazaar.domain.entity;

import com.bhadabazaar.BhadaBazaar.domain.enums.GenderType;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemCategory;
import com.bhadabazaar.BhadaBazaar.domain.enums.VendorStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.bhadabazaar.BhadaBazaar.domain.converter.StringArrayConverter;

@Entity
@Table(name = "vendors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "vendor_name", nullable = false)
    private String vendorName;

    @Column(name = "shop_name", nullable = false)
    private String shopName;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    private String address;

    @Column(name = "primary_phone", nullable = false)
    private String primaryPhone;

    @Column(name = "secondary_phone1")
    private String secondaryPhone1;

    @Column(name = "secondary_phone2")
    private String secondaryPhone2;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_served", nullable = false)
    private GenderType genderServed;
    
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "categories", columnDefinition = "text[]", nullable = false)
    private List<String> categories;

    @Column(name = "store_image_url")
    private String storeImageUrl;
    
    @Column(name = "store_image_public_id")
    private String storeImagePublicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VendorStatus status;

    @Column(name = "yearly_price", nullable = false)
    private BigDecimal yearlyPrice;

    @Column(name = "earnings", nullable = false, columnDefinition = "NUMERIC(12,2) DEFAULT 0")
    @Builder.Default
    private BigDecimal earnings = BigDecimal.ZERO;

    @Column(name = "subscription_start_date", nullable = false)
    private LocalDate subscriptionStartDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
