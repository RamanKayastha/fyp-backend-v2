package com.stitch.story.backend.repositories;

import com.stitch.story.backend.entities.Product;
import com.stitch.story.backend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByVendorOrderByIdDesc(User vendor);
}
