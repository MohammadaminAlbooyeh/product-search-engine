package com.pse.service;

import com.pse.model.document.ProductDocument;
import com.pse.model.entity.PriceEntry;
import com.pse.model.entity.Product;
import com.pse.model.entity.Seller;
import com.pse.model.enums.AvailabilityStatus;
import com.pse.repository.PriceEntryRepository;
import com.pse.repository.ProductRepository;
import com.pse.repository.SellerRepository;
import com.pse.search.ElasticsearchIndexer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductAggregationService {

    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;
    private final PriceEntryRepository priceEntryRepository;
    private final ElasticsearchIndexer elasticsearchIndexer;
    private final PriceHistoryService priceHistoryService;

    @Transactional
    public Product upsertProduct(Product product) {
        Product saved = productRepository.save(product);
        reindex(saved);
        return saved;
    }

    @Transactional
    public PriceEntry upsertPrice(String productId, String sellerName, BigDecimal price,
                                  String url, AvailabilityStatus availability) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new com.pse.exception.ProductNotFoundException(productId));
        Seller seller = sellerRepository.findByName(sellerName)
                .orElseGet(() -> sellerRepository.save(Seller.builder()
                        .name(sellerName)
                        .url(null)
                        .digikala(false)
                        .build()));

        PriceEntry entry = priceEntryRepository.findByProductIdAndSellerId(productId, seller.getId())
                .map(existing -> {
                    existing.setPrice(price);
                    existing.setUrl(url);
                    existing.setAvailability(availability);
                    return existing;
                })
                .orElseGet(() -> PriceEntry.builder()
                        .product(product)
                        .seller(seller)
                        .price(price)
                        .url(url)
                        .availability(availability)
                        .build());

        priceHistoryService.record(entry);
        PriceEntry saved = priceEntryRepository.save(entry);
        reindex(product);
        return saved;
    }

    @Transactional
    public void deleteProduct(String productId) {
        priceEntryRepository.findByProductIdIn(java.util.List.of(productId))
                .forEach(e -> priceEntryRepository.delete(e));
        productRepository.deleteById(productId);
        elasticsearchIndexer.delete(productId);
    }

    private void reindex(Product product) {
        priceEntryRepository.findTopByProductIdOrderByPriceAsc(product.getId()).ifPresentOrElse(entry -> {
            ProductDocument doc = ProductDocument.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .brand(product.getBrand())
                    .category(product.getCategory())
                    .description(product.getDescription())
                    .imageUrl(product.getImageUrl())
                    .price(entry.getPrice())
                    .availability(entry.getAvailability())
                    .sellerName(entry.getSeller().getName())
                    .build();
            elasticsearchIndexer.index(doc);
        }, () -> elasticsearchIndexer.index(ProductDocument.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .category(product.getCategory())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .availability(AvailabilityStatus.OUT_OF_STOCK)
                .build()));
    }
}
