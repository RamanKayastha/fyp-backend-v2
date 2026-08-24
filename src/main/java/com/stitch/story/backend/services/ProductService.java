package com.stitch.story.backend.services;

import com.stitch.story.backend.dtos.ProductDTO;

import java.util.List;

public interface ProductService {
    ProductDTO createProduct(ProductDTO productDTO);

    ProductDTO getProductByID(Long id);

    ProductDTO updateProduct(ProductDTO productDTO, Long id);

    void deleteProduct(Long id);

    List<ProductDTO> getAllProducts();
}
