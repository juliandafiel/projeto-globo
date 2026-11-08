package br.com.checkout.bff.persistence;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Implementação sobre o Spring Data JPA (PostgreSQL). */
@Repository
public class PaymentRepositoryImpl implements PaymentRepositoryI {

    private final PaymentJpaRepository jpa;

    public PaymentRepositoryImpl(PaymentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Payment save(Payment payment) {
        return jpa.save(payment);
    }

    @Override
    public List<Payment> findByCartId(UUID cartId) {
        return jpa.findByCartIdOrderByCreatedAtAsc(cartId);
    }
}
