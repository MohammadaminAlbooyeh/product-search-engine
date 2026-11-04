package com.pse.controller;

import com.pse.exception.GlobalExceptionHandler;
import com.pse.model.entity.PriceAlert;
import com.pse.service.PriceAlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PriceAlertControllerTest {

    private final PriceAlertService service = mock(PriceAlertService.class);
    private MockMvc mockMvc;

    private final JwtAuthenticationToken user = new JwtAuthenticationToken(
            Jwt.withTokenValue("token").header("alg", "HS256")
                    .subject("user@example.com").claim("userId", 5L).build(),
            List.of(new SimpleGrantedAuthority("ROLE_USER")));

    private final JwtAuthenticationToken admin = new JwtAuthenticationToken(
            Jwt.withTokenValue("token").header("alg", "HS256")
                    .subject("admin").claim("userId", 1L).build(),
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PriceAlertController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createUsesEmailAndUserIdFromJwt() throws Exception {
        when(service.create(any(), eq("user@example.com"), eq(5L)))
                .thenReturn(PriceAlert.builder().id(1L).email("user@example.com")
                        .targetPrice(new BigDecimal("10")).active(true).build());

        mockMvc.perform(post("/api/price-alerts").principal(user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"p1\",\"targetPrice\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("user@example.com"));

        verify(service).create(any(), eq("user@example.com"), eq(5L));
    }

    @Test
    void createRejectsMissingProductIdAndPrice() throws Exception {
        mockMvc.perform(post("/api/price-alerts").principal(user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetPrice\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.productId").exists())
                .andExpect(jsonPath("$.errors.targetPrice").exists());

        verifyNoInteractions(service);
    }

    @Test
    void mineListsByUserId() throws Exception {
        when(service.listByUserId(5L)).thenReturn(List.of());

        mockMvc.perform(get("/api/price-alerts/me").principal(user))
                .andExpect(status().isOk());

        verify(service).listByUserId(5L);
    }

    @Test
    void readingSomeoneElsesAlertsIsForbidden() throws Exception {
        mockMvc.perform(get("/api/price-alerts/other@example.com").principal(user))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(service);
    }

    @Test
    void adminCanReadAnyEmail() throws Exception {
        when(service.listByEmail("other@example.com")).thenReturn(List.of());

        mockMvc.perform(get("/api/price-alerts/other@example.com").principal(admin))
                .andExpect(status().isOk());

        verify(service).listByEmail("other@example.com");
    }

    @Test
    void deactivatePassesOwnershipContext() throws Exception {
        mockMvc.perform(delete("/api/price-alerts/7").principal(user))
                .andExpect(status().isNoContent());

        verify(service).deactivate(7L, "user@example.com", false);
    }
}
