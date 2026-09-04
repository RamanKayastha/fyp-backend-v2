package com.stitch.story.backend.repositories;

import com.stitch.story.backend.entities.Order;
import com.stitch.story.backend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByCreatedAtDesc(User user);

    List<Order> findByVendorOrderByCreatedAtDesc(User vendor);

    List<Order> findAllByOrderByCreatedAtDesc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Order o SET o.vendor = null WHERE o.vendor = :vendor")
    void clearVendor(@Param("vendor") User vendor);
}
