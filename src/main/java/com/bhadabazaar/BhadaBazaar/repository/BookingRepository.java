package com.bhadabazaar.BhadaBazaar.repository;

import com.bhadabazaar.BhadaBazaar.domain.entity.Booking;
import com.bhadabazaar.BhadaBazaar.domain.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
    SELECT b FROM Booking b
    WHERE b.vendor.id = :vendorId
      AND b.status = :status
    """)
    Page<Booking> findVendorBookings(
        @Param("vendorId") Long vendorId,
        @Param("status") BookingStatus status,
        Pageable pageable
    );

    Page<Booking> findByVendorIdAndStatusAndFromDate(Long vendorId, BookingStatus status, LocalDate fromDate, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.vendor.id = :vendorId " +
           "AND b.status = 'RETURN_PENDING' " +
           "AND b.toDate <= :deadlineWithBuffer")
    Page<Booking> findReturnPendingBeforeDate(
        @Param("vendorId") Long vendorId,
        @Param("deadlineWithBuffer") LocalDate deadlineWithBuffer,
        Pageable pageable
    );

    List<Booking> findByVendorIdAndStatusAndCustomerPhoneContainingIgnoreCase(Long vendorId, BookingStatus status, String customerPhone);

    List<Booking> findByStatusAndToDate(BookingStatus status, LocalDate toDate);

    java.util.Optional<Booking> findByIdAndVendorId(Long id, Long vendorId);

    /**
     * Returns the ids of any of the given items that are already booked over a range overlapping
     * [fromDate, toDate]. Uses the same overlap semantics as the availability listing query
     * (b.toDate is the stored exclusive end, i.e. user toDate + 1).
     */
    @Query("""
    SELECT DISTINCT bi.item.id FROM BookingItem bi
    WHERE bi.item.id IN :itemIds
      AND bi.booking.status IN :statuses
      AND bi.booking.fromDate <= :toDate
      AND bi.booking.toDate >= :fromDate
    """)
    List<Long> findConflictingItemIds(
        @Param("itemIds") List<Long> itemIds,
        @Param("statuses") List<BookingStatus> statuses,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );

    long countByVendorIdAndStatus(Long vendorId, BookingStatus status);

    // Used by the hard-delete (account purge) flow. Delete booking_items first (see BookingItemRepository).
    @Modifying
    @Query("DELETE FROM Booking b WHERE b.vendor.id = :vendorId")
    void deleteByVendorId(@Param("vendorId") Long vendorId);
}
