package com.stitch.story.backend.services;

import com.stitch.story.backend.dtos.CreateOrderRequest;
import com.stitch.story.backend.dtos.OrderDTO;
import com.stitch.story.backend.dtos.SalesSummaryDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface    OrderService {
    OrderDTO createOrder(CreateOrderRequest request);

    OrderDTO createVerifiedOnlineOrder(CreateOrderRequest request);

    BigDecimal quoteTotal(CreateOrderRequest request);

    OrderDTO getMyOrder(Long id);

    List<OrderDTO> getMyOrders();

    List<OrderDTO> getAllOrders();

    SalesSummaryDTO getSales(String period, LocalDate from, LocalDate to, Long vendorId);

    OrderDTO updateStatus(Long id, String status);
}
