package com.stitch.story.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesSummaryDTO {
    private String period;
    private LocalDate from;
    private LocalDate to;
    private BigDecimal revenue;
    private BigDecimal deliveryFees;
    private BigDecimal commissionRate;
    private BigDecimal vendorRevenue;
    private BigDecimal commission;
    private BigDecimal vendorPayout;
    private long orderCount;
    private long unitsSold;
    private List<SalesPointDTO> dailySales;
    private List<ProductSalesDTO> productSales;
    private List<VendorCommissionDTO> vendorCommissions;
}
