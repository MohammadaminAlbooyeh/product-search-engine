package com.pse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProductSearchEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductSearchEngineApplication.class, args);
    }
}
