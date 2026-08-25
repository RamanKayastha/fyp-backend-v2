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
import com.stitch.story.backend.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal DELIVERY_FEE = BigDecimal.TEN;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Override
    public OrderDTO createOrder(CreateOrderRequest request) {
        User user = getCurrentUser();
        validateDelivery(request);

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod().trim().toUpperCase());
        } catch (Exception exception) {
            throw new BadRequestException("Invalid payment method");
        }

        String fullName = firstNonBlank(request.getFullName(), joinedName(request.getFirstName(), request.getLastName()));
        String region = firstNonBlank(request.getRegion(), request.getState());
        String area = firstNonBlank(request.getArea(), request.getAddress());
        String email = firstNonBlank(request.getEmail(), user.getEmail());
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required");
        }
        String[] nameParts = splitName(fullName);

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
                .deliveryFee(DELIVERY_FEE)
                .build();
        if (order.getItems() == null) {
            order.setItems(new java.util.ArrayList<>());
        }

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

            product.setStock(stock - itemRequest.getQuantity());
            productRepository.save(product);

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            String imageUrl = product.getImages() != null && !product.getImages().isEmpty()
                    ? product.getImages().get(0)
                    : product.getImageUrl();

            boolean customized = Boolean.TRUE.equals(itemRequest.getCustomized());

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(product.getId())
                    .productName(product.getName())
                    .imageUrl(imageUrl)
                    .size(sizeValue)
                    .quantity(itemRequest.getQuantity())
                    .price(product.getPrice())
                    .customized(customized)
                    .previewFront(itemRequest.getPreviewFront())
                    .previewBack(itemRequest.getPreviewBack())
                    .build();
            order.getItems().add(orderItem);
        }

        order.setSubtotal(subtotal);
        order.setTotal(subtotal.add(DELIVERY_FEE));

        return OrderMapper.toDTO(orderRepository.save(order));
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
        requireAdmin();
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(OrderMapper::toDTO)
                .toList();
    }

    @Override
    public OrderDTO updateStatus(Long id, String statusValue) {
        requireAdmin();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus nextStatus;
        try {
            nextStatus = OrderStatus.valueOf(statusValue.trim().toUpperCase().replace(' ', '_'));
        } catch (Exception exception) {
            throw new BadRequestException("Invalid order status");
        }

        if (order.getStatus() == OrderStatus.CANCELLED && nextStatus != OrderStatus.CANCELLED) {
            throw new BadRequestException("A cancelled order cannot be reopened");
        }

        if (order.getStatus() != OrderStatus.CANCELLED && nextStatus == OrderStatus.CANCELLED) {
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
        return OrderMapper.toDTO(saved);
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

    private User requireAdmin() {
        User user = getCurrentUser();
        if (user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Admin access required");
        }
        return user;
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
