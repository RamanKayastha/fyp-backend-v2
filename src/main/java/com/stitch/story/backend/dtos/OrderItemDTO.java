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
public class OrderItemDTO {
    private Long productId;
    private String productName;
    private String imageUrl;
    private String size;
    private Integer quantity;
    private BigDecimal price;
    private boolean customized;
    private String previewFront;
    private String previewBack;
}
