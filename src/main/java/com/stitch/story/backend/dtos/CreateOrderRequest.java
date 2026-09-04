package com.stitch.story.backend.dtos;

import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    private String fullName;
    private String firstName;
    private String lastName;
    private String email;
    private String region;
    private String city;
    private String area;
    private String landmark;
    private Double latitude;
    private Double longitude;
    private String address;
    private String state;
    private String zipCode;
    private String country;
    private String phone;
    private String paymentMethod;
    private List<OrderItemRequest> items;
}
