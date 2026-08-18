package com.pse.model.dto;

import com.pse.model.enums.AvailabilityStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceOption {

    private String sellerName;
    private BigDecimal price;
    private String url;
    private AvailabilityStatus availability;
}
