package br.com.checkout.bff.client.paymentmethod;

import br.com.checkout.bff.domain.PaymentMethodType;

import java.io.Serializable;

/** Meio de pagamento como o serviço de meios de pagamento devolve. */
public record AvailablePaymentMethod(
        String code,
        String name,
        PaymentMethodType type
) implements Serializable {
}
