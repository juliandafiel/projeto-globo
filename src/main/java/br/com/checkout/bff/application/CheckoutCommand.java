package br.com.checkout.bff.application;

/** Pedido de checkout: carrinho, nome do meio escolhido e cartão (nulo no Pix). */
public record CheckoutCommand(String cartId, String paymentMethodName, CardInput card) {
}
