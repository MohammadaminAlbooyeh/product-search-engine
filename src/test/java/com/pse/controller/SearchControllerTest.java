package com.pse.controller;

import com.pse.model.dto.SearchRequest;
import com.pse.model.dto.SearchResponse;
import com.pse.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SearchControllerTest {

    private final SearchService searchService = mock(SearchService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SearchController(searchService)).build();
    }

    @Test
    void mapsAllQueryParametersToRequest() throws Exception {
        when(searchService.search(any())).thenReturn(SearchResponse.builder()
                .total(0).page(2).size(0).results(List.of()).build());

        mockMvc.perform(get("/api/search")
                        .param("q", "macbook")
                        .param("category", "ELECTRONICS")
                        .param("brand", "Apple")
                        .param("minPrice", "100")
                        .param("maxPrice", "900")
                        .param("sortBy", "price_asc")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.page").value(2));

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        org.mockito.Mockito.verify(searchService).search(captor.capture());
        SearchRequest request = captor.getValue();
        assertThat(request.getQuery()).isEqualTo("macbook");
        assertThat(request.getCategory()).isEqualTo(com.pse.model.enums.ProductCategory.ELECTRONICS);
        assertThat(request.getBrand()).isEqualTo("Apple");
        assertThat(request.getMinPrice()).isEqualTo(100.0);
        assertThat(request.getMaxPrice()).isEqualTo(900.0);
        assertThat(request.getSortBy()).isEqualTo("price_asc");
        assertThat(request.getPage()).isEqualTo(2);
        assertThat(request.getSize()).isEqualTo(5);
    }

    @Test
    void defaultsAreAppliedWhenNoParametersSupplied() throws Exception {
        when(searchService.search(any())).thenReturn(SearchResponse.builder()
                .total(0).page(0).size(0).results(List.of()).build());

        mockMvc.perform(get("/api/search"))
                .andExpect(status().isOk());

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        org.mockito.Mockito.verify(searchService).search(captor.capture());
        assertThat(captor.getValue().getSortBy()).isEqualTo("relevance");
        assertThat(captor.getValue().getPage()).isZero();
        assertThat(captor.getValue().getSize()).isEqualTo(20);
    }
}
