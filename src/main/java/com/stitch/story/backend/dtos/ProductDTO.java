package com.stitch.story.backend.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stitch.story.backend.entities.enums.Category;
import com.stitch.story.backend.entities.enums.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDTO {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private List<String> images;
    private Integer stock;
    private Category category;
    private List<Size> sizes;

    @JsonProperty("customizable")
    @JsonAlias({ "isCustomizable" })
    private boolean customizable;
}
