package com.pse.controller;

import com.pse.model.dto.PriceAlertRequest;
import com.pse.model.entity.PriceAlert;
import com.pse.service.PriceAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/price-alerts")
@RequiredArgsConstructor
@Tag(name = "Price Alerts", description = "Price-drop alerts owned by user accounts")
@SecurityRequirement(name = "bearerAuth")
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    @PostMapping
    @Operation(summary = "Create a price-drop alert (owned by the authenticated user)")
    @ApiResponse(responseCode = "201", description = "Alert created")
    public ResponseEntity<PriceAlert> create(@Valid @RequestBody PriceAlertRequest request,
                                             Authentication authentication) {
        String email = request.getEmail();
        Long userId = null;
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            email = jwt.getSubject();
            userId = userIdFrom(jwt);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(priceAlertService.create(request, email, userId));
    }

    @GetMapping("/me")
    @Operation(summary = "List the current user's alerts")
    public List<PriceAlert> mine(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            Long userId = userIdFrom(jwt);
            if (userId != null) {
                return priceAlertService.listByUserId(userId);
            }
        }
        return priceAlertService.listByEmail(authentication.getName());
    }

    @GetMapping("/{email}")
    @Operation(summary = "List alerts for an e-mail (self or admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerts returned"),
            @ApiResponse(responseCode = "403", description = "Not allowed to read another user's alerts")
    })
    public List<PriceAlert> listByEmail(@PathVariable String email, Authentication authentication) {
        assertSelfOrAdmin(email, authentication);
        return priceAlertService.listByEmail(email);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate an alert (owner or admin)")
    @ApiResponse(responseCode = "204", description = "Alert deactivated")
    public ResponseEntity<Void> deactivate(@PathVariable Long id, Authentication authentication) {
        priceAlertService.deactivate(id, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    private Long userIdFrom(Jwt jwt) {
        Object value = jwt.getClaim("userId");
        return value instanceof Number number ? number.longValue() : null;
    }

    private void assertSelfOrAdmin(String email, Authentication authentication) {
        boolean owner = authentication.getName().equalsIgnoreCase(email);
        if (!owner && !isAdmin(authentication)) {
            throw new AccessDeniedException("Not allowed to read alerts for " + email);
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
