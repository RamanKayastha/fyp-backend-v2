package com.stitch.story.backend.entities;

import com.stitch.story.backend.entities.enums.VendorApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class VendorApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    private String shopName;
    private String phone;

    @Column(length = 1000)
    private String address;

    private String idDocument;
    private String payoutAccount;

    @Column(length = 2000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private VendorApplicationStatus status;

    @Column(length = 2000)
    private String adminNote;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = VendorApplicationStatus.PENDING;
        }
    }
}
