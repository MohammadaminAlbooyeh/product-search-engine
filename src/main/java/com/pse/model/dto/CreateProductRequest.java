package com.pse.model.dto;

import com.pse.model.enums.ProductCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "CreateProductRequest")
public class CreateProductRequest {

    @Size(max = 50)
    @Schema(description = "Optional client-supplied id; generated when omitted", example = "p-digi-laptop")
    private String id;

    @NotBlank
    @Size(max = 200)
    @Schema(example = "MacBook Air 13 M3")
    private String name;

    @Size(max = 100)
    private String brand;

    @Schema(description = "Defaults to OTHER", example = "ELECTRONICS")
    private ProductCategory category;

    @Size(max = 500)
    private String imageUrl;

    @Size(max = 2000)
    private String description;
}
