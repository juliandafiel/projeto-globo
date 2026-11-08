package br.com.checkout.bff.gateway;

/** Porta de cobrança. Hoje há só a implementação simulada; um gateway real entraria aqui. */
public interface PaymentGatewayI {

    ChargeResult charge(ChargeRequest request);
}
