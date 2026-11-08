package br.com.checkout.bff.persistence;

public enum CartStatus {
    /** Criado no pré-checkout, aguardando pagamento (inclusive depois de uma recusa). */
    OPEN,
    /** Pagamento aprovado: não aceita outro pagamento. */
    PAID
}
