package br.com.checkout.bff.domain;

/**
 * Pagamento recusado. Tipo próprio para que a transação do checkout grave a tentativa
 * recusada em vez de desfazê-la (ver {@code noRollbackFor} no CheckoutServiceImpl).
 */
public class PaymentDeclinedException extends BusinessException {

    public PaymentDeclinedException(String cartId, String reason) {
        super(ErrorCode.PAYMENT_DECLINED, cartId, "Pagamento recusado: " + reason);
    }
}
