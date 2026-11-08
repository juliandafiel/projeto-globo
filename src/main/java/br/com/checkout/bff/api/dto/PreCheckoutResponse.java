package br.com.checkout.bff.api.dto;

import java.math.BigDecimal;
import java.util.List;

/** Tela de pagamento: o carrinho e os meios que podem ser escolhidos. */
public record PreCheckoutResponse(
        String cartId,
        List<Product> products,
        List<PaymentMethod> paymentMethods
) {
    public record Product(String slug, String name, String description, BigDecimal price, String currency) {
    }

    public record PaymentMethod(String name) {
    }
}
