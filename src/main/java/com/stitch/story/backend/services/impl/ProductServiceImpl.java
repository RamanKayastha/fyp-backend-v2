package com.stitch.story.backend.services.impl;

import com.stitch.story.backend.dtos.ProductDTO;
import com.stitch.story.backend.entities.Product;
import com.stitch.story.backend.entities.User;
import com.stitch.story.backend.entities.enums.ActivityAction;
import com.stitch.story.backend.entities.enums.ActivityEntityType;
import com.stitch.story.backend.entities.enums.Role;
import com.stitch.story.backend.exceptions.ResourceNotFoundException;
import com.stitch.story.backend.exceptions.UnauthorizedException;
import com.stitch.story.backend.mapper.ProductMapper;
import com.stitch.story.backend.repositories.ProductRepository;
import com.stitch.story.backend.repositories.UserRepository;
import com.stitch.story.backend.services.ActivityLogService;
import com.stitch.story.backend.services.ProductService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private UserRepository userRepository;
    private ActivityLogService activityLogService;

    @Override
    public ProductDTO createProduct(ProductDTO productDTO) {
        User actor = requireSeller();
        Product product = ProductMapper.toEntity(productDTO);
        if (actor.getRole() == Role.VENDOR) {
            product.setVendor(actor);
        } else {
            product.setVendor(null);
        }
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
        requireCanManage(product);
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

        product.setCustomizable(productDTO.isCustomizable());

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
        requireCanManage(product);
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

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getMyProducts() {
        User actor = requireSeller();
        if (actor.getRole() == Role.ADMIN) {
            return getAllProducts();
        }
        return productRepository.findByVendorOrderByIdDesc(actor).stream()
                .map(ProductMapper::toDTO)
                .toList();
    }

    private void requireCanManage(Product product) {
        User actor = requireSeller();
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        if (product.getVendor() == null || !product.getVendor().getId().equals(actor.getId())) {
            throw new UnauthorizedException("You can only manage your own products");
        }
    }

    private User requireSeller() {
        User user = getCurrentUser();
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.VENDOR) {
            throw new UnauthorizedException("Seller access required");
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
