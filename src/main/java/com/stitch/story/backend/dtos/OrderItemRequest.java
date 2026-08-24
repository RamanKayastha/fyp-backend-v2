package com.stitch.story.backend.dtos;

import lombok.Data;

@Data
public class OrderItemRequest {
    private Long productId;
    private String size;
    private Integer quantity;
}
