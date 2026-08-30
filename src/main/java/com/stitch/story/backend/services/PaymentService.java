package com.stitch.story.backend.services;

import com.stitch.story.backend.dtos.CreateOrderRequest;
import com.stitch.story.backend.dtos.PaymentInitiateResponse;
import com.stitch.story.backend.dtos.PaymentVerifyRequest;
import com.stitch.story.backend.dtos.PaymentVerifyResponse;

public interface PaymentService {
    PaymentInitiateResponse initiate(CreateOrderRequest request);

    PaymentVerifyResponse verify(PaymentVerifyRequest request);
}
