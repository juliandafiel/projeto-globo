package br.com.checkout.bff.persistence;

import java.util.Optional;
import java.util.UUID;

/** Persistência de carrinhos usada pelos services. */
public interface CartRepositoryI {

    Cart save(Cart cart);

    Optional<Cart> findById(UUID id);

    /**
     * Carrega o carrinho travando a linha até o fim da transação: dois checkouts simultâneos
     * do mesmo carrinho ficam em fila e o segundo já encontra o carrinho pago.
     */
    Optional<Cart> findByIdForUpdate(UUID id);
}
