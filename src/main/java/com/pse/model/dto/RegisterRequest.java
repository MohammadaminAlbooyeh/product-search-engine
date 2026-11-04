package com.pse.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "RegisterRequest", description = "Create a user account and receive a JWT")
public class RegisterRequest {

    @NotBlank
    @Email
    @Schema(example = "buyer@example.com")
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    @Schema(example = "correct-horse-battery-staple")
    private String password;

    @Size(max = 100)
    @Schema(example = "Amin")
    private String displayName;
}
