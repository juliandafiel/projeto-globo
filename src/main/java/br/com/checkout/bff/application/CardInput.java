package br.com.checkout.bff.application;

/** Cartão como chegou do frontend, ainda sem validar. */
public record CardInput(String holderName, String number, String expiry, String cvv) {

    @Override
    public String toString() {
        return "CardInput[holderName=" + holderName + ", number=***, expiry=" + expiry + ", cvv=***]";
    }
}
