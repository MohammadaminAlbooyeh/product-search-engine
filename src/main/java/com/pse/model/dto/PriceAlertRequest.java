package com.pse.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "PriceAlertRequest", description = "Create a price-drop alert for a product")
public class PriceAlertRequest {

    @NotBlank
    @Schema(example = "p-digi-laptop")
    private String productId;

    /** Optional for JWT users (taken from the token); required for the admin/basic-auth flow. */
    @Email
    @Schema(description = "Recipient e-mail. Ignored for JWT users, whose token e-mail wins.", example = "buyer@example.com")
    private String email;

    @NotNull
    @DecimalMin(value = "0.01")
    @Schema(example = "45000000")
    private BigDecimal targetPrice;
}
