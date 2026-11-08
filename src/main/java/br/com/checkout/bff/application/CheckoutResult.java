package br.com.checkout.bff.application;

import br.com.checkout.bff.persistence.Cart;
import br.com.checkout.bff.persistence.Payment;

/** Checkout aprovado: o carrinho pago e o pagamento que o aprovou. */
public record CheckoutResult(Cart cart, Payment payment) {
}
