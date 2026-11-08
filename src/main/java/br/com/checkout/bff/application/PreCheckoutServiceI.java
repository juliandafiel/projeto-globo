package br.com.checkout.bff.application;

import br.com.checkout.bff.persistence.Cart;

import java.util.List;

/** Caso de uso do pré-checkout: monta o carrinho a partir dos planos escolhidos. */
public interface PreCheckoutServiceI {

    Cart createCart(List<String> requestedSlugs);
}
