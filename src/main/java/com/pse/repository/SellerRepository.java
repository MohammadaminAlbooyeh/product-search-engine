package com.pse.repository;

import com.pse.model.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {

    Optional<Seller> findByName(String name);

    List<Seller> findByDigikala(boolean digikala);
}
