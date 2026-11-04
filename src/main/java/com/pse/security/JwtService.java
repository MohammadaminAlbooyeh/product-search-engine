package com.pse.security;

import com.pse.model.entity.User;
import com.pse.model.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;

    @Value("${security.jwt.expiration-seconds:3600}")
    private long expirationSeconds;

    public AuthResponse issue(User user) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .subject(user.getEmail())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirationSeconds))
                .claim("roles", List.of(user.getRole().name()))
                .claim("userId", user.getId());
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            claimsBuilder.claim("name", user.getDisplayName());
        }
        JwtClaimsSet claims = claimsBuilder.build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInSeconds(expirationSeconds)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    public long expirationSeconds() {
        return expirationSeconds;
    }
}
