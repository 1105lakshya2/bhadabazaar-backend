package com.bhadabazaar.BhadaBazaar.repository;

import com.bhadabazaar.BhadaBazaar.domain.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByUsername(String username);
    Optional<Vendor> findById(Long vendorId);
    boolean existsByUsername(String username);
    boolean existsByPrimaryPhone(String primaryPhone);

    // Drives the scheduled finalize job: deletion requests whose grace window has elapsed.
    List<Vendor> findByStatusAndDeletionRequestedAtBefore(
            com.bhadabazaar.BhadaBazaar.domain.enums.VendorStatus status,
            java.time.LocalDateTime cutoff);

    Page<Vendor> findByCity(String city, Pageable pageable);

    // Public listings already filter to APPROVED, which excludes SUSPENDED and DELETED vendors.
    List<Vendor> findByCityAndStatus(String city, com.bhadabazaar.BhadaBazaar.domain.enums.VendorStatus status);

    @Query("SELECT DISTINCT v.city FROM Vendor v WHERE v.status = 'APPROVED'")
    List<String> findAllCities();

    @Query("SELECT DISTINCT v.state, v.city FROM Vendor v WHERE v.status = 'APPROVED'")
    List<Object[]> findDistinctStateCityPairs();

    @Query("SELECT v FROM Vendor v WHERE v.status = 'APPROVED' AND  LOWER(v.shopName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Vendor> searchVendorsList(String query);

    /**
     * Atomically adds to a vendor's earnings in a single SQL statement, so concurrent bookings can't
     * lose an update (the read-modify-write hazard). The DB serializes the row update.
     */
    @Modifying
    @Query("UPDATE Vendor v SET v.earnings = v.earnings + :amount WHERE v.id = :vendorId")
    void addEarnings(@Param("vendorId") Long vendorId, @Param("amount") BigDecimal amount);
}
