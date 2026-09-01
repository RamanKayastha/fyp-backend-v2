package com.stitch.story.backend.dtos;

import lombok.Data;

@Data
public class VendorApplyRequest {
    private String shopName;
    private String phone;
    private String address;
    private String idDocument;
    private String payoutAccount;
    private String note;
}
