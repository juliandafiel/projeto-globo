package br.com.checkout.bff.client.paymentmethod;

import java.util.List;

/** Porta do BFF para o serviço de meios de pagamento. */
public interface PaymentMethodClientI {

    /** Meios de pagamento aceitos no momento. */
    List<AvailablePaymentMethod> findAll();
}
