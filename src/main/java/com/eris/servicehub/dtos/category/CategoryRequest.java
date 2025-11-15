package com.eris.servicehub.dtos.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "Category name cannot be blank")
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String description;
}