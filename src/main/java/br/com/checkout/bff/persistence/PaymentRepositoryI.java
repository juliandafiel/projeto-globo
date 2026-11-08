package br.com.checkout.bff.persistence;

import java.util.List;
import java.util.UUID;

/** Persistência das tentativas de pagamento usada pelos services. */
public interface PaymentRepositoryI {

    Payment save(Payment payment);

    /** Tentativas de um carrinho, da mais antiga para a mais recente. */
    List<Payment> findByCartId(UUID cartId);
}
