package com.stitch.story.backend.repositories;

import com.stitch.story.backend.entities.User;
import com.stitch.story.backend.entities.VendorApplication;
import com.stitch.story.backend.entities.enums.VendorApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorApplicationRepository extends JpaRepository<VendorApplication, Long> {
    Optional<VendorApplication> findFirstByUserOrderByCreatedAtDesc(User user);

    boolean existsByUserAndStatus(User user, VendorApplicationStatus status);

    List<VendorApplication> findAllByOrderByCreatedAtDesc();
}
