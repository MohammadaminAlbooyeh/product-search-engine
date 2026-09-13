package com.pse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pse.model.entity.Seller;
import com.pse.repository.SellerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
@Tag(name = "Sellers")
public class SellerController {

    private final SellerRepository sellerRepository;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(summary = "List all registered sellers")
    public List<Seller> list() {
        return sellerRepository.findAll();
    }

    @PostMapping
    @Operation(summary = "Register a seller (crawler target). Optional crawlConfig enables the HTTP crawler.")
    public ResponseEntity<Seller> create(@RequestBody Map<String, Object> body) {
        Seller seller = Seller.builder()
                .name(String.valueOf(body.get("name")))
                .url((String) body.get("url"))
                .digikala(Boolean.TRUE.equals(body.get("isDigikala")))
                .crawlConfig(serializeCrawlConfig(body.get("crawlConfig")))
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(sellerRepository.save(seller));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sellerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private String serializeCrawlConfig(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof String s) {
            return s.isBlank() ? null : s;
        }
        try {
            return objectMapper.writeValueAsString(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid crawlConfig: " + e.getMessage());
        }
    }
}
