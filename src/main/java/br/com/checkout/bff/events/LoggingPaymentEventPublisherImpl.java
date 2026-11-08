package br.com.checkout.bff.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Sem broker (checkout.events.broker=log): só registra o evento. */
@Component
@ConditionalOnProperty(name = "checkout.events.broker", havingValue = "log", matchIfMissing = true)
public class LoggingPaymentEventPublisherImpl implements PaymentEventPublisherI {

    private static final Logger log = LoggerFactory.getLogger(LoggingPaymentEventPublisherImpl.class);

    @Override
    public void publish(PaymentApprovedEvent event) {
        log.info("Evento payment.approved (sem broker): cart={} valor={} {}", event.cartId(), event.amount(), event.currency());
    }
}
