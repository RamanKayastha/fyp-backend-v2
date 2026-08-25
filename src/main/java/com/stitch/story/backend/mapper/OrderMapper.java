package com.stitch.story.backend.mapper;

import com.stitch.story.backend.dtos.OrderDTO;
import com.stitch.story.backend.dtos.OrderItemDTO;
import com.stitch.story.backend.entities.Order;
import com.stitch.story.backend.entities.OrderItem;

import java.util.List;

public class OrderMapper {

    public static OrderDTO toDTO(Order order) {
        List<OrderItemDTO> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream().map(OrderMapper::toItemDTO).toList();

        int itemCount = items.stream()
                .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                .sum();

        String customerName = order.getFullName();
        if (customerName == null || customerName.isBlank()) {
            customerName = ((order.getFirstName() == null ? "" : order.getFirstName()) + " "
                    + (order.getLastName() == null ? "" : order.getLastName())).trim();
        }

        return OrderDTO.builder()
                .id(order.getId())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .customerName(customerName.isBlank() && order.getUser() != null
                        ? order.getUser().getUsername()
                        : customerName)
                .fullName(order.getFullName())
                .firstName(order.getFirstName())
                .lastName(order.getLastName())
                .email(order.getEmail())
                .address(order.getAddress())
                .city(order.getCity())
                .state(order.getState())
                .region(order.getRegion())
                .area(order.getArea())
                .landmark(order.getLandmark())
                .zipCode(order.getZipCode())
                .country(order.getCountry())
                .phone(order.getPhone())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .subtotal(order.getSubtotal())
                .deliveryFee(order.getDeliveryFee())
                .total(order.getTotal())
                .itemCount(itemCount)
                .createdAt(order.getCreatedAt())
                .items(items)
                .build();
    }
    // to DTO

    private static OrderItemDTO toItemDTO(OrderItem item) {
        return OrderItemDTO.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .imageUrl(item.getImageUrl())
                .size(item.getSize())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .customized(item.isCustomized())
                .previewFront(item.getPreviewFront())
                .previewBack(item.getPreviewBack())
                .build();
    }
}
