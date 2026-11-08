package br.com.checkout.bff.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Tela de sucesso. */
public record CheckoutResponse(
        String cartId,
        String status,
        Instant paidAt,
        Payment payment,
        List<Product> products
) {
    public static final String APPROVED = "APROVADO";

    /** Cartão traz {@code lastDigits}; Pix traz {@code pixCode} no lugar. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Payment(String method, String lastDigits, String pixCode, BigDecimal amount, String currency) {
    }

    public record Product(String slug, String name, BigDecimal price) {
    }
}
