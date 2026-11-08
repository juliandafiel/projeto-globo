package br.com.checkout.bff.application;

/** Caso de uso do checkout: cobra o carrinho com o meio escolhido. */
public interface CheckoutServiceI {

    CheckoutResult checkout(CheckoutCommand command);
}
