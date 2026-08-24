package com.stitch.story.backend.services.impl;

import com.stitch.story.backend.dtos.ActivityLogDTO;
import com.stitch.story.backend.entities.ActivityLog;
import com.stitch.story.backend.entities.User;
import com.stitch.story.backend.entities.enums.ActivityAction;
import com.stitch.story.backend.entities.enums.ActivityEntityType;
import com.stitch.story.backend.entities.enums.Role;
import com.stitch.story.backend.exceptions.UnauthorizedException;
import com.stitch.story.backend.repositories.ActivityLogRepository;
import com.stitch.story.backend.repositories.UserRepository;
import com.stitch.story.backend.services.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    @Override
    public void record(ActivityAction action, ActivityEntityType entityType, Long entityId, String description) {
        User actor = currentAdminOrNull();
        if (actor == null) {
            return;
        }

        activityLogRepository.save(ActivityLog.builder()
                .actorId(actor.getId())
                .actorName(actor.getUsername())
                .actorEmail(actor.getEmail())
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityLogDTO> getAll() {
        User user = currentAdminOrNull();
        if (user == null) {
            throw new UnauthorizedException("Admin access required");
        }

        return activityLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDTO)
                .toList();
    }

    private ActivityLogDTO toDTO(ActivityLog log) {
        return ActivityLogDTO.builder()
                .id(log.getId())
                .actorId(log.getActorId())
                .actorName(log.getActorName())
                .actorEmail(log.getActorEmail())
                .action(log.getAction() != null ? log.getAction().name() : null)
                .entityType(log.getEntityType() != null ? log.getEntityType().name() : null)
                .entityId(log.getEntityId())
                .description(log.getDescription())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private User currentAdminOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        return userRepository.findByEmail(authentication.getName())
                .filter(user -> user.getRole() == Role.ADMIN)
                .orElse(null);
    }
}
