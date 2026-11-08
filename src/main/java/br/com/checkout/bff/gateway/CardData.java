package br.com.checkout.bff.gateway;

/**
 * Dados do cartão só em memória, durante a cobrança. Nunca é persistido e o
 * {@link #toString()} não expõe número nem CVV (evita vazar em log).
 */
public record CardData(String holderName, String number, String expiry, String cvv) {

    public String lastDigits() {
        return number.substring(number.length() - 4);
    }

    @Override
    public String toString() {
        return "CardData[holderName=" + holderName + ", number=****" + lastDigits() + ", expiry=" + expiry + ", cvv=***]";
    }
}
