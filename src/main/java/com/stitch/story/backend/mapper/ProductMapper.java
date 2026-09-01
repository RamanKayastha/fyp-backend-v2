package com.stitch.story.backend.mapper;

import com.stitch.story.backend.dtos.ProductDTO;
import com.stitch.story.backend.entities.Product;
import com.stitch.story.backend.util.ShopNames;

import java.util.ArrayList;
import java.util.List;

public class ProductMapper {

    public static ProductDTO toDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .images(product.getImages() != null ? product.getImages() : List.of())
                .stock(product.getStock())
                .category(product.getCategory())
                .sizes(product.getSizes() != null ? product.getSizes() : List.of())
                .customizable(product.isCustomizable())
                .vendorId(product.getVendor() != null ? product.getVendor().getId() : null)
                .shopName(ShopNames.of(product.getVendor()))
                .build();
    }

    public static Product toEntity(ProductDTO productDTO) {
        List<String> images = productDTO.getImages() != null
                ? new ArrayList<>(productDTO.getImages())
                : new ArrayList<>();

        String imageUrl = productDTO.getImageUrl() != null
                ? productDTO.getImageUrl()
                : (!images.isEmpty() ? images.get(0) : null);

        return Product.builder()
                .id(productDTO.getId())
                .name(productDTO.getName())
                .description(productDTO.getDescription())
                .price(productDTO.getPrice())
                .imageUrl(imageUrl)
                .images(images)
                .stock(productDTO.getStock())
                .category(productDTO.getCategory())
                .sizes(productDTO.getSizes() != null ? new ArrayList<>(productDTO.getSizes()) : new ArrayList<>())
                .customizable(productDTO.isCustomizable())
                .build();
    }
}
