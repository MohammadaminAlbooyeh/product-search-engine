package com.pse.repository;

import com.pse.model.entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    List<PriceAlert> findByEmail(String email);

    List<PriceAlert> findByProductIdAndActiveTrue(String productId);

    List<PriceAlert> findByActiveTrue();
}
