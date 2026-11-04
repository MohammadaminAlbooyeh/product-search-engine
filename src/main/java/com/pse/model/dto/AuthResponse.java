package com.pse.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "AuthResponse", description = "Issued JWT and the user it belongs to")
public class AuthResponse {

    private String token;
    private String tokenType;
    private long expiresInSeconds;
    private String email;
    private String role;
}
