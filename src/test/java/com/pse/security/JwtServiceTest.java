package com.pse.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import com.pse.model.dto.AuthResponse;
import com.pse.model.entity.User;
import com.pse.model.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final SecretKey key = new SecretKeySpec(
            "unit-test-secret-key-at-least-32-bytes!!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    @Test
    void issuesTokenWithSubjectRolesAndUserId() {
        JwtService service = new JwtService(
                new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key)));
        ReflectionTestUtils.setField(service, "expirationSeconds", 120L);

        AuthResponse response = service.issue(User.builder()
                .id(9L).email("buyer@example.com").role(UserRole.USER)
                .displayName("Buyer").build());

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresInSeconds()).isEqualTo(120L);

        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
        Jwt jwt = decoder.decode(response.getToken());
        assertThat(jwt.getSubject()).isEqualTo("buyer@example.com");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
        assertThat(((Number) jwt.getClaim("userId")).longValue()).isEqualTo(9L);
        assertThat(jwt.getClaimAsString("name")).isEqualTo("Buyer");
    }

    @Test
    void adminRoleIsCarriedInToken() {
        JwtService service = new JwtService(
                new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key)));
        ReflectionTestUtils.setField(service, "expirationSeconds", 60L);

        AuthResponse response = service.issue(User.builder()
                .id(1L).email("admin@example.com").role(UserRole.ADMIN).build());

        Jwt jwt = NimbusJwtDecoder.withSecretKey(key).build().decode(response.getToken());
        assertThat(jwt.getClaimAsStringList("roles")).isEqualTo(List.of("ADMIN"));
    }
}
