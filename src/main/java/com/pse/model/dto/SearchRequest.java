package com.pse.model.dto;

import com.pse.model.enums.ProductCategory;
import lombok.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequest {

    private String query;
    private ProductCategory category;
    private String brand;
    private Double minPrice;
    private Double maxPrice;
    private String sortBy;
    private int page;
    private int size;

    public Pageable toPageable() {
        Sort sort = Sort.unsorted();
        if (sortBy != null) {
            if (sortBy.equals("price_asc")) {
                sort = Sort.by("price").ascending();
            } else if (sortBy.equals("price_desc")) {
                sort = Sort.by("price").descending();
            }
        }
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);
    }
}
