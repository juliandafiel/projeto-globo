package br.com.checkout.bff.gateway;

/** Porta de cobrança. Hoje há só a implementação simulada; um gateway real entraria aqui. */
public interface PaymentGateway {

    ChargeResult charge(ChargeRequest request);
}
