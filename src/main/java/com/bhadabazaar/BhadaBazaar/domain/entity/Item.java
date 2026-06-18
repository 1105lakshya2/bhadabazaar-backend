package com.bhadabazaar.BhadaBazaar.domain.entity;

import com.bhadabazaar.BhadaBazaar.domain.enums.ItemCategory;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemGenderType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
// item_code is unique per vendor among non-deleted items only, so a soft-deleted item frees its
// code for reuse. That requires a PARTIAL unique index (WHERE is_deleted = false), which JPA's
// @UniqueConstraint cannot express, so it is defined directly in the DB:
//   CREATE UNIQUE INDEX uq_items_vendor_itemcode_active
//       ON items (vendor_id, item_code) WHERE is_deleted = false;
@Table(name = "items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "item_code", nullable = false)
    private String itemCode;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private ItemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemGenderType gender;

    @Column(name = "price_per_day", nullable = false)
    private BigDecimal pricePerDay;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemImage> images = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
