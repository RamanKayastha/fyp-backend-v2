package com.stitch.story.backend.entities;

import com.stitch.story.backend.entities.enums.ActivityAction;
import com.stitch.story.backend.entities.enums.ActivityEntityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "activity_logs")
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long actorId;
    private String actorName;
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ActivityAction action;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ActivityEntityType entityType;

    private Long entityId;

    @Column(length = 1000)
    private String description;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
