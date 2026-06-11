package com.bhadabazaar.BhadaBazaar.repository;

import com.bhadabazaar.BhadaBazaar.domain.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByUsername(String username);
    Optional<Vendor> findById(Long vendorId);
    boolean existsByUsername(String username);
    boolean existsByPrimaryPhone(String primaryPhone);

    Page<Vendor> findByCity(String city, Pageable pageable);
    
    List<Vendor> findByCityAndStatus(String city, com.bhadabazaar.BhadaBazaar.domain.enums.VendorStatus status);

    @Query("SELECT DISTINCT v.city FROM Vendor v WHERE v.status = 'APPROVED'")
    List<String> findAllCities();

    @Query("SELECT DISTINCT v.state, v.city FROM Vendor v WHERE v.status = 'APPROVED'")
    List<Object[]> findDistinctStateCityPairs();
    
    @Query("SELECT v FROM Vendor v WHERE v.status = 'APPROVED' AND  LOWER(v.shopName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Vendor> searchVendorsList(String query);
}
