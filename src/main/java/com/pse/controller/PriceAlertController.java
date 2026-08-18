package com.pse.controller;

import com.pse.model.dto.PriceAlertRequest;
import com.pse.model.entity.PriceAlert;
import com.pse.service.PriceAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/price-alerts")
@RequiredArgsConstructor
@Tag(name = "Price Alerts")
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    @PostMapping
    @Operation(summary = "Create a price-drop alert")
    public ResponseEntity<PriceAlert> create(@Valid @RequestBody PriceAlertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(priceAlertService.create(request));
    }

    @GetMapping("/{email}")
    @Operation(summary = "List alerts for an email")
    public List<PriceAlert> listByEmail(@PathVariable String email) {
        return priceAlertService.listByEmail(email);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        priceAlertService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
