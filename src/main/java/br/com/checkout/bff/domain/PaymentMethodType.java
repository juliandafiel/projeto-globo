package br.com.checkout.bff.domain;

/** Tipo técnico do meio de pagamento. O nome exibido vem do serviço de meios de pagamento. */
public enum PaymentMethodType {
    CREDIT_CARD,
    DEBIT_CARD,
    PIX;

    public boolean usesCard() {
        return this == CREDIT_CARD || this == DEBIT_CARD;
    }
}
