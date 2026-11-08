package br.com.checkout.bff.events;

/** Para onde vão os eventos de pagamento (RabbitMQ ou só log). */
public interface PaymentEventPublisher {

    void publish(PaymentApprovedEvent event);
}
