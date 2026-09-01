package com.stitch.story.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private Long userId;
    private String customerName;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String address;
    private String city;
    private String state;
    private String region;
    private String area;
    private String landmark;
    private String zipCode;
    private String country;
    private String phone;
    private String paymentMethod;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal total;
    private Integer itemCount;
    private Long vendorId;
    private String shopName;
    private String checkoutGroupId;
    private LocalDateTime createdAt;
    private List<OrderItemDTO> items;
}
