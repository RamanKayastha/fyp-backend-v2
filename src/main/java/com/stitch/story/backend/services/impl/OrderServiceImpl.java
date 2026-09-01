package com.stitch.story.backend.services.impl;

import com.stitch.story.backend.dtos.CreateOrderRequest;
import com.stitch.story.backend.dtos.OrderDTO;
import com.stitch.story.backend.dtos.OrderItemRequest;
import com.stitch.story.backend.entities.Order;
import com.stitch.story.backend.entities.OrderItem;
import com.stitch.story.backend.entities.Product;
import com.stitch.story.backend.entities.User;
import com.stitch.story.backend.entities.enums.ActivityAction;
import com.stitch.story.backend.entities.enums.ActivityEntityType;
import com.stitch.story.backend.entities.enums.OrderStatus;
import com.stitch.story.backend.entities.enums.PaymentMethod;
import com.stitch.story.backend.entities.enums.Role;
import com.stitch.story.backend.exceptions.BadRequestException;
import com.stitch.story.backend.exceptions.ResourceNotFoundException;
import com.stitch.story.backend.exceptions.UnauthorizedException;
import com.stitch.story.backend.mapper.OrderMapper;
import com.stitch.story.backend.repositories.OrderRepository;
import com.stitch.story.backend.repositories.ProductRepository;
import com.stitch.story.backend.repositories.UserRepository;
import com.stitch.story.backend.services.ActivityLogService;
import com.stitch.story.backend.services.EmailService;
import com.stitch.story.backend.services.OrderService;
import com.stitch.story.backend.util.ShopNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal DELIVERY_FEE = new BigDecimal("100");
    private static final BigDecimal COD_FEE = new BigDecimal("50");
    private static final BigDecimal TEXT_LAYER_FEE = new BigDecimal("350");
    private static final BigDecimal IMAGE_LAYER_FEE = new BigDecimal("500");
    private static final BigDecimal GRAPHICS_LAYER_FEE = new BigDecimal("200");

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final EmailService emailService;

    @Override
    public OrderDTO createOrder(CreateOrderRequest request) {
        PaymentMethod paymentMethod = parsePaymentMethod(request.getPaymentMethod());
        if (paymentMethod != PaymentMethod.COD) {
            throw new BadRequestException("Pay with eSewa first");
        }
        return persistOrder(request, paymentMethod);
    }

    @Override
    public OrderDTO createVerifiedOnlineOrder(CreateOrderRequest request) {
        PaymentMethod paymentMethod = parsePaymentMethod(request.getPaymentMethod());
        if (paymentMethod != PaymentMethod.ESEWA) {
            throw new BadRequestException("Invalid payment method");
        }
        return persistOrder(request, paymentMethod);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal quoteTotal(CreateOrderRequest request) {
        User user = getCurrentUser();
        validateDelivery(request);
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }
        if (firstNonBlank(request.getEmail(), user.getEmail()) == null
                || firstNonBlank(request.getEmail(), user.getEmail()).isBlank()) {
            throw new BadRequestException("Email is required");
        }
        return quoteSubtotal(request).add(DELIVERY_FEE);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getMyOrder(Long id) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Unauthorized");
        }
        return OrderMapper.toDTO(order);
    }

    private OrderDTO persistOrder(CreateOrderRequest request, PaymentMethod paymentMethod) {
        User user = getCurrentUser();
        validateDelivery(request);

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        String fullName = firstNonBlank(request.getFullName(), joinedName(request.getFirstName(), request.getLastName()));
        String region = firstNonBlank(request.getRegion(), request.getState());
        String area = firstNonBlank(request.getArea(), request.getAddress());
        String email = firstNonBlank(request.getEmail(), user.getEmail());
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required");
        }
        String[] nameParts = splitName(fullName);
        String checkoutGroupId = UUID.randomUUID().toString();

        List<PreparedLine> lines = new ArrayList<>();
        for (OrderItemRequest itemRequest : request.getItems()) {
            if (itemRequest.getProductId() == null || itemRequest.getQuantity() == null || itemRequest.getQuantity() < 1) {
                throw new BadRequestException("Each item needs a product and quantity");
            }
            if (itemRequest.getSize() == null || itemRequest.getSize().isBlank()) {
                throw new BadRequestException("Please select a size for every item");
            }

            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            int stock = product.getStock() == null ? 0 : product.getStock();
            if (stock < itemRequest.getQuantity()) {
                throw new BadRequestException(product.getName() + " does not have enough stock");
            }

            String sizeValue = itemRequest.getSize().trim().toUpperCase();
            if (product.getSizes() != null && !product.getSizes().isEmpty()) {
                boolean sizeAllowed = product.getSizes().stream()
                        .anyMatch(size -> size.name().equals(sizeValue));
                if (!sizeAllowed) {
                    throw new BadRequestException(product.getName() + " is not available in size " + sizeValue);
                }
            }

            product.setStock(stock - itemRequest.getQuantity());
            productRepository.save(product);

            boolean customized = Boolean.TRUE.equals(itemRequest.getCustomized());
            BigDecimal unitPrice = customized
                    ? customizedUnitPrice(product.getPrice(), itemRequest)
                    : product.getPrice();
            String imageUrl = product.getImages() != null && !product.getImages().isEmpty()
                    ? product.getImages().get(0)
                    : product.getImageUrl();

            lines.add(new PreparedLine(
                    product.getVendor() != null ? product.getVendor().getId() : null,
                    product.getId(),
                    product.getName(),
                    imageUrl,
                    sizeValue,
                    itemRequest.getQuantity(),
                    unitPrice,
                    customized,
                    itemRequest.getPreviewFront(),
                    itemRequest.getPreviewBack()
            ));
        }

        Map<Long, List<PreparedLine>> groups = new LinkedHashMap<>();
        for (PreparedLine line : lines) {
            groups.computeIfAbsent(line.vendorId(), key -> new ArrayList<>()).add(line);
        }

        Order firstSaved = null;
        boolean first = true;
        for (Map.Entry<Long, List<PreparedLine>> entry : groups.entrySet()) {
            User vendor = entry.getKey() == null
                    ? null
                    : userRepository.findById(entry.getKey()).orElse(null);
            BigDecimal fees = first ? orderFees(paymentMethod) : BigDecimal.ZERO;

            Order order = Order.builder()
                    .user(user)
                    .fullName(fullName.trim())
                    .firstName(nameParts[0])
                    .lastName(nameParts[1])
                    .email(email.trim())
                    .region(region.trim())
                    .state(region.trim())
                    .city(request.getCity().trim())
                    .area(area.trim())
                    .landmark(request.getLandmark().trim())
                    .address(area.trim())
                    .zipCode(blankToNull(request.getZipCode()))
                    .country(firstNonBlank(request.getCountry(), "Nepal").trim())
                    .phone(request.getPhone().trim())
                    .paymentMethod(paymentMethod)
                    .status(OrderStatus.PENDING)
                    .deliveryFee(fees)
                    .checkoutGroupId(checkoutGroupId)
                    .vendor(vendor)
                    .shopName(ShopNames.of(vendor))
                    .build();
            if (order.getItems() == null) {
                order.setItems(new ArrayList<>());
            }

            BigDecimal subtotal = BigDecimal.ZERO;
            for (PreparedLine line : entry.getValue()) {
                BigDecimal lineTotal = line.unitPrice().multiply(BigDecimal.valueOf(line.quantity()));
                subtotal = subtotal.add(lineTotal);
                order.getItems().add(OrderItem.builder()
                        .order(order)
                        .productId(line.productId())
                        .productName(line.productName())
                        .imageUrl(line.imageUrl())
                        .size(line.size())
                        .quantity(line.quantity())
                        .price(line.unitPrice())
                        .customized(line.customized())
                        .previewFront(line.previewFront())
                        .previewBack(line.previewBack())
                        .build());
            }

            order.setSubtotal(subtotal);
            order.setTotal(subtotal.add(fees));
            Order saved = orderRepository.save(order);
            activityLogService.record(
                    ActivityAction.CREATE,
                    ActivityEntityType.ORDER,
                    saved.getId(),
                    "Placed order #" + saved.getId() + " for " + ShopNames.of(vendor)
            );
            if (firstSaved == null) {
                firstSaved = saved;
            }
            first = false;
        }

        return OrderMapper.toDTO(firstSaved);
    }

    private record PreparedLine(
            Long vendorId,
            Long productId,
            String productName,
            String imageUrl,
            String size,
            int quantity,
            BigDecimal unitPrice,
            boolean customized,
            String previewFront,
            String previewBack
    ) {
    }

    private BigDecimal quoteSubtotal(CreateOrderRequest request) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.getItems()) {
            if (itemRequest.getProductId() == null || itemRequest.getQuantity() == null || itemRequest.getQuantity() < 1) {
                throw new BadRequestException("Each item needs a product and quantity");
            }
            if (itemRequest.getSize() == null || itemRequest.getSize().isBlank()) {
                throw new BadRequestException("Please select a size for every item");
            }

            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            int stock = product.getStock() == null ? 0 : product.getStock();
            if (stock < itemRequest.getQuantity()) {
                throw new BadRequestException(product.getName() + " does not have enough stock");
            }

            String sizeValue = itemRequest.getSize().trim().toUpperCase();
            if (product.getSizes() != null && !product.getSizes().isEmpty()) {
                boolean sizeAllowed = product.getSizes().stream()
                        .anyMatch(size -> size.name().equals(sizeValue));
                if (!sizeAllowed) {
                    throw new BadRequestException(product.getName() + " is not available in size " + sizeValue);
                }
            }

            boolean customized = Boolean.TRUE.equals(itemRequest.getCustomized());
            BigDecimal unitPrice = customized
                    ? customizedUnitPrice(product.getPrice(), itemRequest)
                    : product.getPrice();
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }
        return subtotal;
    }

    private PaymentMethod parsePaymentMethod(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Payment method is required");
        }
        try {
            return PaymentMethod.valueOf(value.trim().toUpperCase());
        } catch (Exception exception) {
            throw new BadRequestException("Invalid payment method");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getMyOrders() {
        User user = getCurrentUser();
        return orderRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(OrderMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        User actor = getCurrentUser();
        if (actor.getRole() == Role.ADMIN) {
            return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                    .map(OrderMapper::toDTO)
                    .toList();
        }
        if (actor.getRole() == Role.VENDOR) {
            return orderRepository.findByVendorOrderByCreatedAtDesc(actor).stream()
                    .map(OrderMapper::toDTO)
                    .toList();
        }
        throw new UnauthorizedException("Admin access required");
    }

    @Override
    public OrderDTO updateStatus(Long id, String statusValue) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        requireCanManageOrder(order);

        OrderStatus nextStatus;
        try {
            nextStatus = OrderStatus.valueOf(statusValue.trim().toUpperCase().replace(' ', '_'));
        } catch (Exception exception) {
            throw new BadRequestException("Invalid order status");
        }

        OrderStatus currentStatus = order.getStatus() == null ? OrderStatus.PENDING : order.getStatus();
        if (!currentStatus.canTransitionTo(nextStatus)) {
            throw new BadRequestException("Order status can only move one step forward");
        }
        if (currentStatus == nextStatus) {
            return OrderMapper.toDTO(order);
        }

        if (currentStatus != OrderStatus.CANCELLED && nextStatus == OrderStatus.CANCELLED) {
            restoreStock(order);
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(nextStatus);
        Order saved = orderRepository.save(order);
        activityLogService.record(
                ActivityAction.STATUS_CHANGE,
                ActivityEntityType.ORDER,
                saved.getId(),
                "Updated order #" + saved.getId() + " from " + previousStatus + " to " + nextStatus
        );
        if (nextStatus == OrderStatus.OUT_FOR_DELIVERY) {
            notifyOutForDelivery(saved);
        }
        return OrderMapper.toDTO(saved);
    }

    private void notifyOutForDelivery(Order order) {
        String to = firstNonBlank(order.getEmail(), order.getUser() != null ? order.getUser().getEmail() : null);
        if (to == null || to.isBlank()) {
            log.warn("Skipped out-for-delivery email for order #{}: no customer email", order.getId());
            return;
        }
        try {
            emailService.sendOutForDelivery(to.trim(), order);
        } catch (Exception exception) {
            log.warn("Could not send out-for-delivery email for order #{}: {}", order.getId(), exception.getMessage());
        }
    }

    private BigDecimal orderFees(PaymentMethod paymentMethod) {
        return paymentMethod == PaymentMethod.COD ? DELIVERY_FEE.add(COD_FEE) : DELIVERY_FEE;
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getProductId() == null) {
                continue;
            }
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                int stock = product.getStock() == null ? 0 : product.getStock();
                product.setStock(stock + item.getQuantity());
                productRepository.save(product);
            });
        }
    }

    private void validateDelivery(CreateOrderRequest request) {
        String fullName = firstNonBlank(request.getFullName(), joinedName(request.getFirstName(), request.getLastName()));
        String region = firstNonBlank(request.getRegion(), request.getState());
        String area = firstNonBlank(request.getArea(), request.getAddress());

        requireText(fullName, "Full name is required");
        requireText(region, "Region is required");
        requireText(request.getPhone(), "Phone number is required");
        requireText(request.getCity(), "City is required");
        requireText(area, "Area is required");
        requireText(request.getLandmark(), "Street address / landmark is required");
        requireText(request.getPaymentMethod(), "Payment method is required");
    }

    private BigDecimal customizedUnitPrice(BigDecimal basePrice, OrderItemRequest itemRequest) {
        return basePrice
                .add(TEXT_LAYER_FEE.multiply(BigDecimal.valueOf(nonNegative(itemRequest.getTextCount()))))
                .add(IMAGE_LAYER_FEE.multiply(BigDecimal.valueOf(nonNegative(itemRequest.getImageCount()))))
                .add(GRAPHICS_LAYER_FEE.multiply(BigDecimal.valueOf(nonNegative(itemRequest.getGraphicsCount()))));
    }

    private int nonNegative(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private String joinedName(String firstName, String lastName) {
        return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    }

    private String[] splitName(String fullName) {
        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        if (space < 0) {
            return new String[] { trimmed, "" };
        }
        return new String[] { trimmed.substring(0, space), trimmed.substring(space + 1).trim() };
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void requireCanManageOrder(Order order) {
        User actor = getCurrentUser();
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        if (actor.getRole() == Role.VENDOR
                && order.getVendor() != null
                && order.getVendor().getId().equals(actor.getId())) {
            return;
        }
        throw new UnauthorizedException("You can only update your own orders");
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Unauthorized");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Unauthorized"));
    }
}
