package com.pse.controller;

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

    @GetMapping
    @Operation(summary = "List all registered sellers")
    public List<Seller> list() {
        return sellerRepository.findAll();
    }

    @PostMapping
    @Operation(summary = "Register a seller (crawler target)")
    public ResponseEntity<Seller> create(@RequestBody Map<String, Object> body) {
        Seller seller = Seller.builder()
                .name(String.valueOf(body.get("name")))
                .url((String) body.get("url"))
                .digikala(Boolean.TRUE.equals(body.get("isDigikala")))
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(sellerRepository.save(seller));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sellerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
