package br.com.checkout.bff.events;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Publica o pagamento aprovado na exchange {@value RabbitEventsConfig#EXCHANGE}. */
@Component
@ConditionalOnProperty(name = "checkout.events.broker", havingValue = "rabbit")
public class RabbitPaymentEventPublisher implements PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitPaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(PaymentApprovedEvent event) {
        rabbitTemplate.convertAndSend(RabbitEventsConfig.EXCHANGE, RabbitEventsConfig.ROUTING_KEY_APPROVED, event);
    }
}
