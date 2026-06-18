package com.bhadabazaar.BhadaBazaar.domain.entity;

import com.bhadabazaar.BhadaBazaar.domain.enums.ItemImageType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "item_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;
    
    @Column(name = "image_public_id")
    private String imagePublicId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "image_type", nullable = false)
    private ItemImageType imageType;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
