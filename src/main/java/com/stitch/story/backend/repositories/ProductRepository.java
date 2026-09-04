package com.stitch.story.backend.repositories;

import com.stitch.story.backend.entities.Product;
import com.stitch.story.backend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByVendorOrderByIdDesc(User vendor);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET p.vendor = null WHERE p.vendor = :vendor")
    void clearVendor(@Param("vendor") User vendor);
}
