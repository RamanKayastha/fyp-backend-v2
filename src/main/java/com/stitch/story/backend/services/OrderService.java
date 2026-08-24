package com.stitch.story.backend.services;

import com.stitch.story.backend.dtos.CreateOrderRequest;
import com.stitch.story.backend.dtos.OrderDTO;

import java.util.List;

public interface OrderService {
    OrderDTO createOrder(CreateOrderRequest request);

    List<OrderDTO> getMyOrders();

    List<OrderDTO> getAllOrders();

    OrderDTO updateStatus(Long id, String status);
}
