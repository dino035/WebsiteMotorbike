package com.nguyen.motoparts.cart.repository;

import com.nguyen.motoparts.cart.entity.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, String> {

    Optional<ShoppingCart> findByAccountId(String accountId);
}
