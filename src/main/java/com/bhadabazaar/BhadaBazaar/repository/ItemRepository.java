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
    boolean existsByName(String name);
    Page<Item> findByVendorId(Long vendorId, Pageable pageable);

    @Query(value = """
    SELECT i.* FROM items i
    WHERE i.vendor_id = :vendorId
    AND i.is_active = true
    AND i.is_deleted = false

    AND (CAST(:category AS text) IS NULL OR i.category = CAST(:category AS text))
    AND (CAST(:gender AS text) IS NULL OR i.gender = CAST(:gender AS text))
    AND (CAST(:searchByName AS text) IS NULL OR i.name ILIKE CONCAT('%', CAST(:searchByName AS text), '%'))
    AND (CAST(:searchByID AS text) IS NULL OR i.item_code ILIKE CONCAT('%', CAST(:searchByID AS text), '%'))

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

    AND (CAST(:category AS text) IS NULL OR i.category = CAST(:category AS text))
    AND (CAST(:gender AS text) IS NULL OR i.gender = CAST(:gender AS text))
    AND (CAST(:searchByName AS text) IS NULL OR i.name ILIKE CONCAT('%', CAST(:searchByName AS text), '%'))
    AND (CAST(:searchByID AS text) IS NULL OR i.item_code ILIKE CONCAT('%', CAST(:searchByID AS text), '%'))

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
            @Param("searchByName") String searchByName,
            @Param("searchByID") String searchByID,
            Pageable pageable
    );


    
    // For vendor management - filter items
    @Query("""
    SELECT i FROM Item i
    WHERE i.vendor.id = :vendorId
    AND i.isDeleted = false

    AND (CAST(:category AS string) IS NULL OR i.category = :category)
    AND (CAST(:gender AS string) IS NULL OR i.gender = :gender)
    AND (
        CAST(:searchByName AS string) IS NULL OR 
        LOWER(i.name) LIKE LOWER(CONCAT('%', CAST(:searchByName AS string), '%'))
    )
    AND (
        CAST(:searchByID AS string) IS NULL OR 
        LOWER(i.itemCode) LIKE LOWER(CONCAT('%', CAST(:searchByID AS string), '%'))
    )
    """)
    Page<Item> findVendorItems(
        @Param("vendorId") Long vendorId,
        @Param("category") ItemCategory category,
        @Param("gender") ItemGenderType gender,
        @Param("searchByName") String searchByName,
        @Param("searchByID") String searchByID,
        Pageable pageable
    );

    @Query("SELECT i FROM Item i WHERE i.vendor.id = :vendorId AND i.itemCode = :itemCode AND i.isDeleted = false")
    java.util.Optional<Item> findByVendorIdAndItemCode(@Param("vendorId") Long vendorId, @Param("itemCode") String itemCode);

    long countByVendorIdAndIsDeletedFalse(Long vendorId);
}
