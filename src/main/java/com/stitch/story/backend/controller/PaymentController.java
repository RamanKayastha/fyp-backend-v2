package com.stitch.story.backend.controller;

import com.stitch.story.backend.dtos.CreateOrderRequest;
import com.stitch.story.backend.dtos.PaymentInitiateResponse;
import com.stitch.story.backend.dtos.PaymentVerifyRequest;
import com.stitch.story.backend.dtos.PaymentVerifyResponse;
import com.stitch.story.backend.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiateResponse> initiate(@RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(paymentService.initiate(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentVerifyResponse> verify(@RequestBody PaymentVerifyRequest request) {
        return ResponseEntity.ok(paymentService.verify(request));
    }
}
