package com.pse.model.dto;

import com.pse.model.enums.ProductCategory;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductComparisonDTO {

    private String productId;
    private String name;
    private ProductCategory category;
    private String imageUrl;
    private BigDecimal cheapestPrice;
    private BigDecimal averagePrice;
    private List<PriceOption> prices;
}
