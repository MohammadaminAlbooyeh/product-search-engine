package com.pse.model.dto;

import com.pse.model.document.ProductDocument;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponse {

    private long total;
    private int page;
    private int size;
    private List<ProductDocument> results;
}
