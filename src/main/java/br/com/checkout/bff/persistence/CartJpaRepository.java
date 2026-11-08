package br.com.checkout.bff.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA de carrinhos. Só o CartRepositoryImpl usa diretamente. */
public interface CartJpaRepository extends JpaRepository<Cart, UUID> {

    /**
     * Carrega o carrinho travando a linha até o fim da transação: dois checkouts simultâneos
     * do mesmo carrinho ficam em fila e o segundo já encontra o carrinho pago.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.id = :id")
    Optional<Cart> findByIdForUpdate(@Param("id") UUID id);
}
