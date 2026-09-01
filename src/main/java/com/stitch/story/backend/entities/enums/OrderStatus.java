package com.stitch.story.backend.entities.enums;

import java.util.List;

public enum OrderStatus {
    PENDING,
    PACKING,
    READY_TO_SHIP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    private static final List<OrderStatus> FLOW = List.of(
            PENDING,
            PACKING,
            READY_TO_SHIP,
            OUT_FOR_DELIVERY,
            DELIVERED
    );

    public boolean canTransitionTo(OrderStatus next) {
        if (next == null || this == next) {
            return this == next;
        }
        if (this == CANCELLED || this == DELIVERED) {
            return false;
        }
        if (next == CANCELLED) {
            return true;
        }
        int from = FLOW.indexOf(this);
        int to = FLOW.indexOf(next);
        return from >= 0 && to == from + 1;
    }
}
