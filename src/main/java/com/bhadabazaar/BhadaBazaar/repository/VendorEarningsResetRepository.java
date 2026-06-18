package com.bhadabazaar.BhadaBazaar.repository;

import com.bhadabazaar.BhadaBazaar.domain.entity.VendorEarningsReset;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorEarningsResetRepository extends JpaRepository<VendorEarningsReset, Long> {

    /** Most recent resets first, for the given vendor. Limit via the Pageable. */
    List<VendorEarningsReset> findByVendorIdOrderByResetAtDesc(Long vendorId, Pageable pageable);

    // Used by the hard-delete (account purge) flow.
    @Modifying
    @Query("DELETE FROM VendorEarningsReset r WHERE r.vendor.id = :vendorId")
    void deleteByVendorId(@Param("vendorId") Long vendorId);
}
