package com.stitch.story.backend.entities;

import com.stitch.story.backend.entities.enums.PaymentMethod;
import com.stitch.story.backend.entities.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "pending_payments")
public class PendingPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "amount_paisa    ", nullable = false)
    private Long amountPaisa;

    @Column(unique = true)
    private String pidx;

    @Column(nullable = false, unique = true)
    private String transactionUuid;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String orderPayload;

    private Long orderId;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = PaymentStatus.INITIATED;
        }
        if (amountPaisa == null && amount != null) {
            amountPaisa = amount.movePointRight(2).longValue();
        }
    }
}
