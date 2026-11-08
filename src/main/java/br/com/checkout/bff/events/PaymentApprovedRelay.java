package br.com.checkout.bff.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Repassa o evento ao broker só depois do commit: nada é publicado para um pagamento
 * que não chegou a ser gravado. Falha no broker não desfaz o pagamento já aprovado.
 */
@Component
public class PaymentApprovedRelay {

    private static final Logger log = LoggerFactory.getLogger(PaymentApprovedRelay.class);

    private final PaymentEventPublisher publisher;

    public PaymentApprovedRelay(PaymentEventPublisher publisher) {
        this.publisher = publisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApproved(PaymentApprovedEvent event) {
        try {
            publisher.publish(event);
        } catch (RuntimeException e) {
            log.error("Falha ao publicar pagamento aprovado do carrinho {}: {}", event.cartId(), e.getMessage());
        }
    }
}
