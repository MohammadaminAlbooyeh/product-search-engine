package com.pse.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceAlertRequest {

    @NotBlank
    private String productId;

    @NotBlank
    @Email
    private String email;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal targetPrice;
}
