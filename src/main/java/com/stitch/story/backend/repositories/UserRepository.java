package com.stitch.story.backend.repositories;

import com.stitch.story.backend.entities.User;
import com.stitch.story.backend.entities.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);
}
