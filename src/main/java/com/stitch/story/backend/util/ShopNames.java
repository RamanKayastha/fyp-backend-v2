package com.stitch.story.backend.util;

import com.stitch.story.backend.entities.User;

public final class ShopNames {
    public static final String PLATFORM = "Stitch & Story";

    private ShopNames() {
    }

    public static String of(User vendor) {
        if (vendor == null) {
            return PLATFORM;
        }
        if (vendor.getShopName() != null && !vendor.getShopName().isBlank()) {
            return vendor.getShopName().trim();
        }
        if (vendor.getUsername() != null && !vendor.getUsername().isBlank()) {
            return vendor.getUsername().trim();
        }
        return PLATFORM;
    }
}
