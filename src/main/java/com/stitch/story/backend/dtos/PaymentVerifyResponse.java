package com.stitch.story.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerifyResponse {
    private Long orderId;
    private boolean customized;
    private String paymentMethod;
    private OrderDTO order;
}
