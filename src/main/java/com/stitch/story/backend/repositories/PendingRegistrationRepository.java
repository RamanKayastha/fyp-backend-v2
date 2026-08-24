package com.stitch.story.backend.repositories;

import com.stitch.story.backend.entities.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration,Long> {
    Optional<PendingRegistration> findByEmail(String email);
    void deleteByEmail(String email);
}
