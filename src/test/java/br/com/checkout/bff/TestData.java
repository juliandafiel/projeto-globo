package br.com.checkout.bff;

import br.com.checkout.bff.client.paymentmethod.AvailablePaymentMethod;
import br.com.checkout.bff.client.product.CatalogProduct;
import br.com.checkout.bff.domain.PaymentMethodType;

import java.math.BigDecimal;
import java.util.List;

/** Mesmo catálogo e meios de pagamento dos mocks, para os testes não dependerem de HTTP. */
public final class TestData {

    public static final String CREDIT = "Cartão de Crédito";
    public static final String DEBIT = "Cartão de Débito";
    public static final String PIX = "Pix";

    public static final List<CatalogProduct> PRODUCTS = List.of(
            new CatalogProduct("basico", "Plano Básico", "Assinatura mensal com catálogo essencial", new BigDecimal("19.90"), "BRL"),
            new CatalogProduct("premium", "Plano Premium", "Assinatura mensal com catálogo completo", new BigDecimal("39.90"), "BRL"),
            new CatalogProduct("familia", "Plano Família", "Assinatura mensal para até 5 perfis", new BigDecimal("59.90"), "BRL")
    );

    public static final List<AvailablePaymentMethod> PAYMENT_METHODS = List.of(
            new AvailablePaymentMethod("credit_card", CREDIT, PaymentMethodType.CREDIT_CARD),
            new AvailablePaymentMethod("debit_card", DEBIT, PaymentMethodType.DEBIT_CARD),
            new AvailablePaymentMethod("pix", PIX, PaymentMethodType.PIX)
    );

    private TestData() {
    }
}
