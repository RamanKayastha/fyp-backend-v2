package com.stitch.story.backend.services.impl;

import com.stitch.story.backend.dtos.ProductDTO;
import com.stitch.story.backend.entities.Product;
import com.stitch.story.backend.entities.enums.ActivityAction;
import com.stitch.story.backend.entities.enums.ActivityEntityType;
import com.stitch.story.backend.exceptions.ResourceNotFoundException;
import com.stitch.story.backend.mapper.ProductMapper;
import com.stitch.story.backend.repositories.ProductRepository;
import com.stitch.story.backend.services.ActivityLogService;
import com.stitch.story.backend.services.ProductService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private ProductRepository productRepository;
    private ActivityLogService activityLogService;

    @Override
    public ProductDTO createProduct(ProductDTO productDTO) {
        Product product = ProductMapper.toEntity(productDTO);
        Product savedProduct = productRepository.save(product);
        activityLogService.record(
                ActivityAction.CREATE,
                ActivityEntityType.PRODUCT,
                savedProduct.getId(),
                "Created product \"" + savedProduct.getName() + "\""
        );
        return ProductMapper.toDTO(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductByID(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return ProductMapper.toDTO(product);
    }

    @Override
    public ProductDTO updateProduct(ProductDTO productDTO, Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setStock(productDTO.getStock());
        product.setCategory(productDTO.getCategory());

        if (product.getImages() == null) {
            product.setImages(new ArrayList<>());
        }
        product.getImages().clear();
        if (productDTO.getImages() != null) {
            product.getImages().addAll(productDTO.getImages());
        }

        String imageUrl = productDTO.getImageUrl() != null
                ? productDTO.getImageUrl()
                : (!product.getImages().isEmpty() ? product.getImages().get(0) : null);
        product.setImageUrl(imageUrl);

        if (product.getSizes() == null) {
            product.setSizes(new ArrayList<>());
        }
        product.getSizes().clear();
        if (productDTO.getSizes() != null) {
            product.getSizes().addAll(productDTO.getSizes());
        }

        Product savedProduct = productRepository.save(product);
        activityLogService.record(
                ActivityAction.UPDATE,
                ActivityEntityType.PRODUCT,
                savedProduct.getId(),
                "Updated product \"" + savedProduct.getName() + "\""
        );
        return ProductMapper.toDTO(savedProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        String name = product.getName();
        productRepository.delete(product);
        activityLogService.record(
                ActivityAction.DELETE,
                ActivityEntityType.PRODUCT,
                id,
                "Deleted product \"" + name + "\""
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream().map(ProductMapper::toDTO).collect(Collectors.toList());
    }
}
