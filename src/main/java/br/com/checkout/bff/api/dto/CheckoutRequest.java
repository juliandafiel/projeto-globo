package br.com.checkout.bff.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code cartId} é texto (e não UUID) de propósito: id malformado vira "carrinho não encontrado"
 * no padrão frontendError, igual a um id que não existe.
 */
public record CheckoutRequest(
        @NotBlank @Size(max = 64) String cartId,
        @NotNull @Valid PaymentMethod paymentMethod
) {
    public record PaymentMethod(@NotBlank @Size(max = 60) String name, Card card) {
    }

    public record Card(String holderName, String number, String expiry, String cvv) {

        @Override
        public String toString() {
            return "Card[holderName=" + holderName + ", number=***, expiry=" + expiry + ", cvv=***]";
        }
    }
}
