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

    @Query("SELECT b FROM Booking b WHERE b.vendor.id = :vendorId " +
           "AND (:status IS NULL OR b.status = :status) " +
           "AND (cast(:fromDate as date) IS NULL OR b.fromDate >= :fromDate) " +
           "AND (cast(:toDate as date) IS NULL OR b.toDate <= :toDate)")
    Page<Booking> findVendorBookings(
        @Param("vendorId") Long vendorId,
        @Param("status") BookingStatus status,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        Pageable pageable
    );

    @Query("SELECT b FROM Booking b WHERE b.vendor.id = :vendorId " +
           "AND b.fromDate <= :date " +
           "AND b.toDate >= :datePlusTwo")
    Page<Booking> findVendorBookingsByDate(
        @Param("vendorId") Long vendorId,
        @Param("date") LocalDate date,
        @Param("datePlusTwo") LocalDate datePlusTwo,
        Pageable pageable
    );

    @Query("SELECT b FROM Booking b WHERE b.vendor.id = :vendorId " +
           "AND b.status = 'RETURN_PENDING' " +
           "AND b.toDate <= :deadlineWithBuffer")
    Page<Booking> findReturnPendingBeforeDate(
        @Param("vendorId") Long vendorId,
        @Param("deadlineWithBuffer") LocalDate deadlineWithBuffer,
        Pageable pageable
    );

    Page<Booking> findByVendorIdAndCustomerPhoneContainingIgnoreCase(Long vendorId, String customerPhone);

    Page<Booking> findByStatusAndToDate(BookingStatus status, LocalDate toDate);
}
