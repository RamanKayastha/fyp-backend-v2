package com.stitch.story.backend.services;

import com.stitch.story.backend.dtos.CreateOrderRequest;
import com.stitch.story.backend.dtos.OrderDTO;

import java.math.BigDecimal;
import java.util.List;

public interface    OrderService {
    OrderDTO createOrder(CreateOrderRequest request);

    OrderDTO createVerifiedOnlineOrder(CreateOrderRequest request);

    BigDecimal quoteTotal(CreateOrderRequest request);

    OrderDTO getMyOrder(Long id);

    List<OrderDTO> getMyOrders();

    List<OrderDTO> getAllOrders();

    OrderDTO updateStatus(Long id, String status);
}
