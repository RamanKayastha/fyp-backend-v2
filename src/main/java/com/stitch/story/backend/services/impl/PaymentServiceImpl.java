package com.stitch.story.backend.services.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stitch.story.backend.dtos.CreateOrderRequest;
import com.stitch.story.backend.dtos.OrderDTO;
import com.stitch.story.backend.dtos.PaymentInitiateResponse;
import com.stitch.story.backend.dtos.PaymentVerifyRequest;
import com.stitch.story.backend.dtos.PaymentVerifyResponse;
import com.stitch.story.backend.entities.PendingPayment;
import com.stitch.story.backend.entities.User;
import com.stitch.story.backend.entities.enums.PaymentMethod;
import com.stitch.story.backend.entities.enums.PaymentStatus;
import com.stitch.story.backend.exceptions.BadRequestException;
import com.stitch.story.backend.exceptions.UnauthorizedException;
import com.stitch.story.backend.repositories.PendingPaymentRepository;
import com.stitch.story.backend.repositories.UserRepository;
import com.stitch.story.backend.services.OrderService;
import com.stitch.story.backend.services.PaymentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final String TEST_ESEWA_SECRET = "8gBm/:&EnhH.1/q";

    private final OrderService orderService;
    private final PendingPaymentRepository pendingPaymentRepository;
    private final UserRepository userRepository;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final RestClient restClient = RestClient.create();

    @Value("${payment.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${esewa.form-url:https://rc-epay.esewa.com.np/api/epay/main/v2/form}")
    private String esewaFormUrl;

    @Value("${esewa.status-url:https://rc.esewa.com.np/api/epay/transaction/status/}")
    private String esewaStatusUrl;

    @Value("${esewa.product-code:EPAYTEST}")
    private String esewaProductCode;

    @Value("${esewa.secret-key:}")
    private String esewaSecretKey;

    @Override
    public PaymentInitiateResponse initiate(CreateOrderRequest request) {
        User user = getCurrentUser();
        request.setPaymentMethod(PaymentMethod.ESEWA.name());

        BigDecimal total = orderService.quoteTotal(request).setScale(2, RoundingMode.HALF_UP);
        String transactionUuid = UUID.randomUUID().toString();

        pendingPaymentRepository.save(PendingPayment.builder()
                .user(user)
                .method(PaymentMethod.ESEWA)
                .status(PaymentStatus.INITIATED)
                .amount(total)
                .amountPaisa(total.movePointRight(2).longValueExact())
                .transactionUuid(transactionUuid)
                .orderPayload(jsonMapper.writeValueAsString(request))
                .build());

        String amount = total.toPlainString();
        String message = "total_amount=" + amount
                + ",transaction_uuid=" + transactionUuid
                + ",product_code=" + esewaProductCode;

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("amount", amount);
        fields.put("tax_amount", "0");
        fields.put("total_amount", amount);
        fields.put("transaction_uuid", transactionUuid);
        fields.put("product_code", esewaProductCode);
        fields.put("product_service_charge", "0");
        fields.put("product_delivery_charge", "0");
        fields.put("success_url", frontendUrl() + "/payment/success");
        fields.put("failure_url", frontendUrl() + "/payment/failure");
        fields.put("signed_field_names", "total_amount,transaction_uuid,product_code");
        fields.put("signature", hmacSha256(esewaSecret(), message));

        return PaymentInitiateResponse.builder()
                .gateway(PaymentMethod.ESEWA.name())
                .formUrl(esewaFormUrl)
                .formFields(fields)
                .build();
    }

    @Override
    public PaymentVerifyResponse verify(PaymentVerifyRequest request) {
        User user = getCurrentUser();
        if (isBlank(request.getData())) {
            throw new BadRequestException("Missing eSewa payment data");
        }

        EsewaCallbackData callback = decodeEsewaData(request.getData());
        if (isBlank(callback.getTransactionUuid())) {
            throw new BadRequestException("eSewa did not return a transaction id");
        }

        PendingPayment pending = pendingPaymentRepository.findByTransactionUuid(callback.getTransactionUuid())
                .orElseThrow(() -> new BadRequestException("Payment session not found"));
        if (pending.getUser() == null || !pending.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Unauthorized");
        }

        if (pending.getStatus() == PaymentStatus.COMPLETED && pending.getOrderId() != null) {
            return toVerifyResponse(orderService.getMyOrder(pending.getOrderId()));
        }

        String amount = pending.getAmount().toPlainString();
        URI statusUri = UriComponentsBuilder.fromUriString(esewaStatusUrl)
                .queryParam("product_code", esewaProductCode)
                .queryParam("total_amount", amount)
                .queryParam("transaction_uuid", pending.getTransactionUuid())
                .build(true)
                .toUri();

        EsewaStatusResponse status = getJson(statusUri);
        if (status == null || !isEsewaComplete(status.getStatus())) {
            throw new BadRequestException("eSewa payment is not completed");
        }
        if (!amountMatches(status.getTotalAmount(), pending.getAmount())) {
            throw new BadRequestException("Paid amount does not match the order total");
        }

        CreateOrderRequest orderRequest = jsonMapper.readValue(pending.getOrderPayload(), CreateOrderRequest.class);
        orderRequest.setPaymentMethod(PaymentMethod.ESEWA.name());
        OrderDTO order = orderService.createVerifiedOnlineOrder(orderRequest);

        pending.setStatus(PaymentStatus.COMPLETED);
        pending.setOrderId(order.getId());
        pending.setCompletedAt(LocalDateTime.now());
        pendingPaymentRepository.save(pending);

        return toVerifyResponse(order);
    }

    private PaymentVerifyResponse toVerifyResponse(OrderDTO order) {
        boolean customized = order.getItems() != null && order.getItems().stream()
                .anyMatch(item -> item.isCustomized()
                        || (item.getPreviewFront() != null && !item.getPreviewFront().isBlank())
                        || (item.getPreviewBack() != null && !item.getPreviewBack().isBlank()));
        return PaymentVerifyResponse.builder()
                .orderId(order.getId())
                .customized(customized)
                .paymentMethod(order.getPaymentMethod())
                .order(order)
                .build();
    }

    private EsewaStatusResponse getJson(URI uri) {
        try {
            String response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(status -> status.isError(), (httpRequest, httpResponse) -> {
                        String errorBody = new String(httpResponse.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw new BadRequestException(gatewayError(errorBody));
                    })
                    .body(String.class);
            if (isBlank(response)) {
                throw new BadRequestException("Empty response from eSewa");
            }
            return jsonMapper.readValue(response, EsewaStatusResponse.class);
        } catch (BadRequestException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            Throwable current = exception;
            while (current != null) {
                if (current instanceof BadRequestException badRequestException) {
                    throw badRequestException;
                }
                current = current.getCause();
            }
            throw new BadRequestException("Could not reach eSewa");
        }
    }

    private EsewaCallbackData decodeEsewaData(String encodedData) {
        try {
            byte[] decoded;
            try {
                decoded = Base64.getUrlDecoder().decode(encodedData);
            } catch (IllegalArgumentException exception) {
                decoded = Base64.getDecoder().decode(encodedData);
            }
            return jsonMapper.readValue(new String(decoded, StandardCharsets.UTF_8), EsewaCallbackData.class);
        } catch (RuntimeException exception) {
            throw new BadRequestException("Invalid eSewa payment data");
        }
    }

    private String hmacSha256(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new BadRequestException("Could not sign eSewa payment");
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Unauthorized");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Unauthorized"));
    }

    private String esewaSecret() {
        return isBlank(esewaSecretKey) ? TEST_ESEWA_SECRET : esewaSecretKey;
    }

    private String frontendUrl() {
        return frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    private boolean isEsewaComplete(String status) {
        return "COMPLETE".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status);
    }

    private boolean amountMatches(String paidAmount, BigDecimal expected) {
        if (isBlank(paidAmount)) {
            return false;
        }
        try {
            return new BigDecimal(paidAmount.replace(",", "")).compareTo(expected) == 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private String gatewayError(String errorBody) {
        if (isBlank(errorBody)) {
            return "eSewa error";
        }
        return "eSewa error: " + (errorBody.length() > 300 ? errorBody.substring(0, 300) : errorBody);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EsewaCallbackData {
        private String status;
        @JsonProperty("transaction_uuid")
        private String transactionUuid;
        @JsonProperty("total_amount")
        private String totalAmount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EsewaStatusResponse {
        private String status;
        @JsonProperty("total_amount")
        private String totalAmount;
        @JsonProperty("transaction_uuid")
        private String transactionUuid;
    }
}
