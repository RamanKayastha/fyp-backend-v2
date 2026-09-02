package com.stitch.story.backend.services.impl;

import com.stitch.story.backend.dtos.VendorApplicationDTO;
import com.stitch.story.backend.dtos.VendorApplyRequest;
import com.stitch.story.backend.dtos.VendorReviewRequest;
import com.stitch.story.backend.entities.User;
import com.stitch.story.backend.entities.VendorApplication;
import com.stitch.story.backend.entities.enums.ActivityAction;
import com.stitch.story.backend.entities.enums.ActivityEntityType;
import com.stitch.story.backend.entities.enums.Role;
import com.stitch.story.backend.entities.enums.VendorApplicationStatus;
import com.stitch.story.backend.exceptions.BadRequestException;
import com.stitch.story.backend.exceptions.ResourceNotFoundException;
import com.stitch.story.backend.exceptions.UnauthorizedException;
import com.stitch.story.backend.repositories.UserRepository;
import com.stitch.story.backend.repositories.VendorApplicationRepository;
import com.stitch.story.backend.services.ActivityLogService;
import com.stitch.story.backend.services.VendorProofStorage;
import com.stitch.story.backend.services.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VendorServiceImpl implements VendorService {

    private final VendorApplicationRepository vendorApplicationRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final VendorProofStorage vendorProofStorage;

    @Override
    public VendorApplicationDTO apply(VendorApplyRequest request, MultipartFile proof) {
        User user = getCurrentUser();
        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Admins cannot apply as vendors");
        }
        if (user.getRole() == Role.VENDOR) {
            throw new BadRequestException("You are already a vendor");
        }
        if (vendorApplicationRepository.existsByUserAndStatus(user, VendorApplicationStatus.PENDING)) {
            throw new BadRequestException("Your vendor application is already pending review");
        }

        requireText(request.getShopName(), "Shop name is required");
        requireText(request.getPhone(), "Phone number is required");
        requireText(request.getAddress(), "Address is required");
        requireText(request.getIdDocument(), "ID / PAN number is required");
        requireText(request.getPayoutAccount(), "Payout account is required");

        VendorProofStorage.StoredProof storedProof = vendorProofStorage.store(proof);

        VendorApplication saved = vendorApplicationRepository.save(VendorApplication.builder()
                .user(user)
                .shopName(request.getShopName().trim())
                .phone(request.getPhone().trim())
                .address(request.getAddress().trim())
                .idDocument(request.getIdDocument().trim())
                .payoutAccount(request.getPayoutAccount().trim())
                .proofStoredName(storedProof.storedName())
                .proofOriginalName(storedProof.originalName())
                .proofContentType(storedProof.contentType())
                .note(blankToNull(request.getNote()))
                .status(VendorApplicationStatus.PENDING)
                .build());

        activityLogService.record(
                ActivityAction.CREATE,
                ActivityEntityType.VENDOR,
                saved.getId(),
                "Vendor application from " + user.getEmail()
        );
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VendorApplicationDTO myApplication() {
        return vendorApplicationRepository.findFirstByUserOrderByCreatedAtDesc(getCurrentUser())
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorApplicationDTO> listApplications() {
        requireAdmin();
        return vendorApplicationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public VendorApplicationDTO review(Long id, VendorReviewRequest request) {
        requireAdmin();
        VendorApplication application = vendorApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if (application.getStatus() != VendorApplicationStatus.PENDING) {
            throw new BadRequestException("This application has already been reviewed");
        }

        VendorApplicationStatus nextStatus;
        try {
            nextStatus = VendorApplicationStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (Exception exception) {
            throw new BadRequestException("Review must be APPROVED or REJECTED");
        }
        if (nextStatus == VendorApplicationStatus.PENDING) {
            throw new BadRequestException("Review must be APPROVED or REJECTED");
        }

        User applicant = application.getUser();
        application.setStatus(nextStatus);
        application.setAdminNote(blankToNull(request.getAdminNote()));
        application.setReviewedAt(LocalDateTime.now());

        if (nextStatus == VendorApplicationStatus.APPROVED) {
            applicant.setRole(Role.VENDOR);
            applicant.setShopName(application.getShopName());
            if ((applicant.getContact() == null || applicant.getContact().isBlank())
                    && application.getPhone() != null) {
                applicant.setContact(application.getPhone());
            }
            if ((applicant.getAddress() == null || applicant.getAddress().isBlank())
                    && application.getAddress() != null) {
                applicant.setAddress(application.getAddress());
            }
            userRepository.save(applicant);
        }

        VendorApplication saved = vendorApplicationRepository.save(application);
        activityLogService.record(
                ActivityAction.UPDATE,
                ActivityEntityType.VENDOR,
                saved.getId(),
                nextStatus.name() + " vendor application for " + applicant.getEmail()
        );
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProofDocument loadProofDocument(Long id) {
        VendorApplication application = vendorApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        User current = getCurrentUser();
        boolean owner = application.getUser() != null && application.getUser().getId().equals(current.getId());
        if (current.getRole() != Role.ADMIN && !owner) {
            throw new UnauthorizedException("Admin access required");
        }
        if (application.getProofStoredName() == null || application.getProofStoredName().isBlank()) {
            throw new ResourceNotFoundException("Document not found");
        }
        return new ProofDocument(
                vendorProofStorage.load(application.getProofStoredName()),
                application.getProofOriginalName() == null ? "document" : application.getProofOriginalName(),
                application.getProofContentType()
        );
    }

    private VendorApplicationDTO toDTO(VendorApplication application) {
        User user = application.getUser();
        return VendorApplicationDTO.builder()
                .id(application.getId())
                .userId(user != null ? user.getId() : null)
                .username(user != null ? user.getUsername() : null)
                .email(user != null ? user.getEmail() : null)
                .shopName(application.getShopName())
                .phone(application.getPhone())
                .address(application.getAddress())
                .idDocument(application.getIdDocument())
                .payoutAccount(application.getPayoutAccount())
                .proofDocumentName(application.getProofOriginalName())
                .proofUploaded(application.getProofStoredName() != null && !application.getProofStoredName().isBlank())
                .note(application.getNote())
                .status(application.getStatus() != null ? application.getStatus().name() : null)
                .adminNote(application.getAdminNote())
                .createdAt(application.getCreatedAt())
                .reviewedAt(application.getReviewedAt())
                .build();
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private User requireAdmin() {
        User user = getCurrentUser();
        if (user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Admin access required");
        }
        return user;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Unauthorized");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Unauthorized"));
    }
}
