package com.bhadabazaar.BhadaBazaar.repository;

import com.bhadabazaar.BhadaBazaar.domain.entity.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {

    // Used by the hard-delete (account purge) flow: removed before bookings/items to satisfy FKs.
    @Modifying
    @Query("DELETE FROM BookingItem bi WHERE bi.booking.id IN "
            + "(SELECT b.id FROM Booking b WHERE b.vendor.id = :vendorId)")
    void deleteByVendorId(@Param("vendorId") Long vendorId);
}
