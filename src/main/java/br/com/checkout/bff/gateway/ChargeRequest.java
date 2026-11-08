package br.com.checkout.bff.gateway;

import br.com.checkout.bff.domain.PaymentMethodType;

import java.math.BigDecimal;
import java.util.UUID;

/** Pedido de cobrança. {@code card} é nulo no Pix. */
public record ChargeRequest(UUID cartId, PaymentMethodType type, BigDecimal amount, String currency, CardData card) {
}
