package com.bhadabazaar.BhadaBazaar.repository;

import com.bhadabazaar.BhadaBazaar.domain.entity.Item;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemCategory;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemGenderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    Page<Item> findByVendorId(Long vendorId, Pageable pageable);

    @Query(value = """
    SELECT i.* FROM items i
    WHERE i.vendor_id = :vendorId
    AND i.is_active = true
    AND i.is_deleted = false

    AND (:category IS NULL OR i.category = CAST(:category AS item_category))
    AND (:gender IS NULL OR i.gender = CAST(:gender AS gender_type))
    AND (:search IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')))

    AND NOT EXISTS (
        SELECT 1 FROM booking_items bi
        JOIN bookings b ON bi.booking_id = b.id
        WHERE bi.item_id = i.id
        AND b.status IN ('BOOKED', 'RETURN_PENDING')
        AND (b.from_date <= :toDate AND b.to_date >= :fromDate)
    )
    """,
    countQuery = """
    SELECT COUNT(*)
    FROM items i
    WHERE i.vendor_id = :vendorId
    AND i.is_active = true
    AND i.is_deleted = false

    AND (:category IS NULL OR i.category = CAST(:category AS item_category))
    AND (:gender IS NULL OR i.gender = CAST(:gender AS gender_type))
    AND (:search IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')))

    AND NOT EXISTS (
        SELECT 1 FROM booking_items bi
        JOIN bookings b ON bi.booking_id = b.id
        WHERE bi.item_id = i.id
        AND b.status IN ('BOOKED', 'RETURN_PENDING')
        AND (b.from_date <= :toDate AND b.to_date >= :fromDate)
    )
    """,
    nativeQuery = true)
    Page<Item> findAvailableItems(
        @Param("vendorId") Long vendorId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("category") String category,
        @Param("gender") String gender,
        @Param("search") String search,
        Pageable pageable
);

    
    // For vendor management - filter items
    @Query("""
    SELECT i FROM Item i
    WHERE i.vendor.id = :vendorId
    AND i.isDeleted = false

    AND (:category IS NULL OR i.category = :category)
    AND (:gender IS NULL OR i.gender = :gender)
    AND (:search IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<Item> findVendorItems(
        @Param("vendorId") Long vendorId,
        @Param("category") ItemCategory category,
        @Param("gender") ItemGenderType gender,
        @Param("search") String search,
        Pageable pageable
    );

    @Query("SELECT i FROM Item i WHERE i.vendor.id = :vendorId AND i.itemCode = :itemCode AND i.isDeleted = false")
    java.util.Optional<Item> findByVendorIdAndItemCode(@Param("vendorId") Long vendorId, @Param("itemCode") String itemCode);
}
