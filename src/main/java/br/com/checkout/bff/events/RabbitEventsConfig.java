package br.com.checkout.bff.events;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Topologia do RabbitMQ (checkout.events.broker=rabbit). */
@Configuration
@ConditionalOnProperty(name = "checkout.events.broker", havingValue = "rabbit")
public class RabbitEventsConfig {

    public static final String EXCHANGE = "checkout.events";
    public static final String ROUTING_KEY_APPROVED = "payment.approved";
    public static final String QUEUE_SUBSCRIPTION_ACTIVATION = "checkout.subscription-activation";
    public static final String DEAD_LETTER_QUEUE = QUEUE_SUBSCRIPTION_ACTIVATION + ".dlq";

    @Bean
    TopicExchange checkoutEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue subscriptionActivationQueue() {
        return QueueBuilder.durable(QUEUE_SUBSCRIPTION_ACTIVATION)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    Queue subscriptionActivationDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding subscriptionActivationBinding(Queue subscriptionActivationQueue, TopicExchange checkoutEventsExchange) {
        return BindingBuilder.bind(subscriptionActivationQueue).to(checkoutEventsExchange).with(ROUTING_KEY_APPROVED);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
