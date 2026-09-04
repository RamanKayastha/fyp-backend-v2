package com.stitch.story.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorCommissionDTO {
    private Long vendorId;
    private String shopName;
    private BigDecimal revenue;
    private BigDecimal commission;
    private BigDecimal vendorPayout;
    private long orderCount;
    private long unitsSold;
}
