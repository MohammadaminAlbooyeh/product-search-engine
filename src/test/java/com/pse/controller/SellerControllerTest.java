package com.pse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pse.model.entity.Seller;
import com.pse.repository.SellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SellerControllerTest {

    private final SellerRepository repository = mock(SellerRepository.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new SellerController(repository, new ObjectMapper())).build();
    }

    @Test
    void listReturnsSellers() throws Exception {
        when(repository.findAll()).thenReturn(List.of(
                Seller.builder().id(1L).name("digikala").digikala(true).build()));

        mockMvc.perform(get("/api/sellers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("digikala"))
                .andExpect(jsonPath("$[0].digikala").value(true));
    }

    @Test
    void createSerializesCrawlConfigObject() throws Exception {
        when(repository.save(any(Seller.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/sellers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "techshop",
                                  "url": "https://shop.example.com",
                                  "crawlConfig": {"mode":"html","itemSelector":"div.card"}
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("techshop"));

        ArgumentCaptor<Seller> captor = ArgumentCaptor.forClass(Seller.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCrawlConfig())
                .contains("\"mode\":\"html\"")
                .contains("\"itemSelector\":\"div.card\"");
    }

    @Test
    void createBlankCrawlConfigStoresNull() throws Exception {
        when(repository.save(any(Seller.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/sellers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"shop\",\"crawlConfig\":\"  \"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<Seller> captor = ArgumentCaptor.forClass(Seller.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCrawlConfig()).isNull();
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/sellers/9"))
                .andExpect(status().isNoContent());

        verify(repository).deleteById(9L);
    }
}
