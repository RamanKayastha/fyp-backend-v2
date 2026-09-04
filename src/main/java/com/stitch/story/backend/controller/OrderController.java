package com.stitch.story.backend.controller;

import com.stitch.story.backend.dtos.CreateOrderRequest;
import com.stitch.story.backend.dtos.OrderDTO;
import com.stitch.story.backend.dtos.OrderStatusRequest;
import com.stitch.story.backend.dtos.SalesSummaryDTO;
import com.stitch.story.backend.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody CreateOrderRequest request) {
        return new ResponseEntity<>(orderService.createOrder(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getMyOrders() {
        return ResponseEntity.ok(orderService.getMyOrders());
    }

    @GetMapping("/all")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/sales")
    public ResponseEntity<SalesSummaryDTO> getSales(
            @RequestParam(required = false, defaultValue = "week") String period,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long vendorId
    ) {
        return ResponseEntity.ok(orderService.getSales(period, from, to, vendorId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateStatus(
            @PathVariable Long id,
            @RequestBody OrderStatusRequest request
    ) {
        return ResponseEntity.ok(orderService.updateStatus(id, request.getStatus()));
    }
}
