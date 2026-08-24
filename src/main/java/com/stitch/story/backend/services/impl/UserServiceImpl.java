package com.stitch.story.backend.services.impl;

import com.stitch.story.backend.dtos.UserDTO;
import com.stitch.story.backend.entities.User;
import com.stitch.story.backend.entities.enums.ActivityAction;
import com.stitch.story.backend.entities.enums.ActivityEntityType;
import com.stitch.story.backend.entities.enums.AuthProvider;
import com.stitch.story.backend.entities.enums.Role;
import com.stitch.story.backend.exceptions.BadRequestException;
import com.stitch.story.backend.exceptions.ResourceNotFoundException;
import com.stitch.story.backend.exceptions.UnauthorizedException;
import com.stitch.story.backend.mapper.UserMapper;
import com.stitch.story.backend.repositories.UserRepository;
import com.stitch.story.backend.services.ActivityLogService;
import com.stitch.story.backend.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private ActivityLogService activityLogService;

    @Override
    public UserDTO createUser(UserDTO userDTO) {
        requireAdmin();

        if (userDTO.getEmail() == null || userDTO.getEmail().isBlank()) {
            throw new BadRequestException("Email is required");
        }
        if (userDTO.getPassword() == null || userDTO.getPassword().isBlank()) {
            throw new BadRequestException("Password is required");
        }
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists.");
        }

        User user = UserMapper.toEntity(userDTO);
        user.setId(null);
        user.setEmail(userDTO.getEmail().trim());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setRole(userDTO.getRole() != null ? userDTO.getRole() : Role.USER);
        user.setAuthProvider(AuthProvider.LOCAL);

        User saved = userRepository.save(user);
        activityLogService.record(
                ActivityAction.CREATE,
                ActivityEntityType.USER,
                saved.getId(),
                "Created user " + saved.getEmail() + " as " + saved.getRole()
        );
        return UserMapper.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserByID(Long id) {
        User currentUser = getCurrentUser();
        if (!isAdmin(currentUser) && !currentUser.getId().equals(id)) {
            throw new UnauthorizedException("You can only view your own profile");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserMapper.toDTO(user);
    }

    @Override
    public UserDTO updateUser(UserDTO userDTO, Long id) {
        User currentUser = getCurrentUser();
        boolean admin = isAdmin(currentUser);
        boolean self = currentUser.getId().equals(id);

        if (!admin && !self) {
            throw new UnauthorizedException("You can only update your own profile");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setUsername(userDTO.getUsername());
        user.setContact(userDTO.getContact());
        user.setAddress(userDTO.getAddress());

        if (admin && userDTO.getRole() != null) {
            if (self && userDTO.getRole() != Role.ADMIN) {
                throw new BadRequestException("You cannot remove your own admin role");
            }
            user.setRole(userDTO.getRole());
        }

        User saved = userRepository.save(user);
        boolean profileUpdate = self;
        activityLogService.record(
                ActivityAction.UPDATE,
                profileUpdate ? ActivityEntityType.PROFILE : ActivityEntityType.USER,
                saved.getId(),
                profileUpdate
                        ? "Updated own profile"
                        : "Updated user " + saved.getEmail()
        );
        return UserMapper.toDTO(saved);
    }

    @Override
    public void deleteUser(Long id) {
        User currentUser = requireAdmin();
        if (currentUser.getId().equals(id)) {
            throw new BadRequestException("You cannot delete your own account");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String email = user.getEmail();
        userRepository.delete(user);
        activityLogService.record(
                ActivityAction.DELETE,
                ActivityEntityType.USER,
                id,
                "Deleted user " + email
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getALlUsers() {
        requireAdmin();
        return userRepository.findAll().stream()
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());
    }

    private User requireAdmin() {
        User currentUser = getCurrentUser();
        if (!isAdmin(currentUser)) {
            throw new UnauthorizedException("Admin access required");
        }
        return currentUser;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Unauthorized");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Unauthorized"));
    }

    private boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }
}
