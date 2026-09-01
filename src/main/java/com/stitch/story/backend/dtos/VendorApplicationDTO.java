package com.stitch.story.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorApplicationDTO {
    private Long id;
    private Long userId;
    private String username;
    private String email;
    private String shopName;
    private String phone;
    private String address;
    private String idDocument;
    private String payoutAccount;
    private String note;
    private String status;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
