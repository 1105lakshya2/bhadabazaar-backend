package com.bhadabazaar.BhadaBazaar.repository;

import com.bhadabazaar.BhadaBazaar.domain.entity.Booking;
import com.bhadabazaar.BhadaBazaar.domain.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    long countByVendorIdAndStatus(Long vendorId, BookingStatus status);
}
