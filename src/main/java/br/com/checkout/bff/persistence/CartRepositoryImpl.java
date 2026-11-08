package br.com.checkout.bff.persistence;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Implementação sobre o Spring Data JPA (PostgreSQL). */
@Repository
public class CartRepositoryImpl implements CartRepositoryI {

    private final CartJpaRepository jpa;

    public CartRepositoryImpl(CartJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Cart save(Cart cart) {
        return jpa.save(cart);
    }

    @Override
    public Optional<Cart> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Cart> findByIdForUpdate(UUID id) {
        return jpa.findByIdForUpdate(id);
    }
}
