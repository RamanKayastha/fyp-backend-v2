package com.stitch.story.backend.dtos;

import lombok.Data;

@Data
public class VendorReviewRequest {
    private String status;
    private String adminNote;
}
