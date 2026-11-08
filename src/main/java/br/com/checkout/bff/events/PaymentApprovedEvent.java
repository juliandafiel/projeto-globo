package br.com.checkout.bff.events;

import br.com.checkout.bff.domain.PaymentMethodType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Pagamento aprovado. Sem dado de cartão nenhum. */
public record PaymentApprovedEvent(
        UUID cartId,
        UUID paymentId,
        String paymentMethod,
        PaymentMethodType paymentMethodType,
        BigDecimal amount,
        String currency,
        Instant paidAt,
        List<String> productSlugs
) {
}
