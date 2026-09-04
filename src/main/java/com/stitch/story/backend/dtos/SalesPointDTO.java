package com.stitch.story.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesPointDTO {
    private LocalDate date;
    private String label;
    private BigDecimal revenue;
    private long orderCount;
    private long unitsSold;
}
