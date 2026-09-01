package com.stitch.story.backend.services;

import com.stitch.story.backend.entities.Order;
import com.stitch.story.backend.entities.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendOtp(String email, String otp) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(email);

        message.setSubject(
                "Verification Code"
        );

        message.setText(
                "Your verification code is: "
                        + otp +
                        "\n\nExpires in 5 minutes."
        );

        mailSender.send(message);
    }

    public void sendOutForDelivery(String to, Order order) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your order #" + order.getId() + " is out for delivery");
        message.setText(outForDeliveryBody(order));
        mailSender.send(message);
    }

    private String outForDeliveryBody(Order order) {
        String shop = order.getShopName() == null || order.getShopName().isBlank()
                ? "Stitch & Story"
                : order.getShopName();
        String name = firstNonBlank(order.getFullName(), joinedName(order.getFirstName(), order.getLastName()));
        String address = String.join(", ",
                java.util.stream.Stream.of(order.getLandmark(), firstNonBlank(order.getArea(), order.getAddress()),
                                order.getCity(), firstNonBlank(order.getRegion(), order.getState()))
                        .filter(value -> value != null && !value.isBlank())
                        .toList());

        StringBuilder items = new StringBuilder();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                items.append("- ")
                        .append(item.getProductName() == null ? "Item" : item.getProductName());
                if (item.getSize() != null && !item.getSize().isBlank()) {
                    items.append(" (").append(item.getSize()).append(")");
                }
                items.append(" x ").append(item.getQuantity() == null ? 1 : item.getQuantity())
                        .append("\n");
            }
        }

        return "Your order is out for delivery.\n\n"
                + "Order #" + order.getId() + " from " + shop + "\n\n"
                + "Items:\n" + items
                + "\nDelivering to:\n"
                + (name == null ? "" : name + "\n")
                + (address.isBlank() ? "" : address + "\n")
                + (order.getPhone() == null || order.getPhone().isBlank() ? "" : "Phone: " + order.getPhone() + "\n")
                + "\nTotal: Rs. " + order.getTotal() + "\n\n"
                + "You can also check this order in My Orders.";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String joinedName(String first, String last) {
        String joined = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
        return joined.isEmpty() ? null : joined;
    }
}
