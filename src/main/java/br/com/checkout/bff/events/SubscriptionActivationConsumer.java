package br.com.checkout.bff.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Processamento assíncrono depois da aprovação (ativar a assinatura, enviar recibo...).
 * Aqui é simulado com log; num sistema real seria outro serviço ouvindo a mesma fila.
 * Mensagem que falhar vai para a fila .dlq em vez de ficar em loop.
 */
@Component
@ConditionalOnProperty(name = "checkout.events.broker", havingValue = "rabbit")
public class SubscriptionActivationConsumer {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionActivationConsumer.class);

    @RabbitListener(queues = RabbitEventsConfig.QUEUE_SUBSCRIPTION_ACTIVATION)
    public void onPaymentApproved(PaymentApprovedEvent event) {
        log.info("Ativando assinatura: cart={} planos={} pago em {}", event.cartId(), event.productSlugs(), event.paidAt());
    }
}
