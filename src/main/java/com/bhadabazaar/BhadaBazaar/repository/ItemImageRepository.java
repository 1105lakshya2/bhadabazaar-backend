package com.bhadabazaar.BhadaBazaar.repository;

import com.bhadabazaar.BhadaBazaar.domain.entity.ItemImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemImageRepository extends JpaRepository<ItemImage, Long> {

    // --- Used by the hard-delete (account purge) flow ---

    @Query("SELECT img.imagePublicId FROM ItemImage img "
            + "WHERE img.item.vendor.id = :vendorId AND img.imagePublicId IS NOT NULL")
    List<String> findImagePublicIdsByVendorId(@Param("vendorId") Long vendorId);

    @Modifying
    @Query("DELETE FROM ItemImage img WHERE img.item.id IN "
            + "(SELECT i.id FROM Item i WHERE i.vendor.id = :vendorId)")
    void deleteByVendorId(@Param("vendorId") Long vendorId);
}
