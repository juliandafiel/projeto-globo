package br.com.checkout.bff.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data JPA de pagamentos. Só o PaymentRepositoryImpl usa diretamente. */
public interface PaymentJpaRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByCartIdOrderByCreatedAtAsc(UUID cartId);
}
