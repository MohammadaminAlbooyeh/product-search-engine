package com.pse.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sellers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "url", length = 500)
    private String url;

    @Column(name = "is_digikala", nullable = false)
    private boolean digikala;

    /**
     * Optional JSON crawl configuration. When set, the HTTP/HTML crawler adapter
     * scrapes this seller instead of the fixture adapters. See
     * {@code com.pse.crawler.config.SellerCrawlConfig}.
     */
    @Column(name = "crawl_config", columnDefinition = "text")
    private String crawlConfig;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
