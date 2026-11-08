package br.com.checkout.bff.mock;

import br.com.checkout.bff.client.paymentmethod.AvailablePaymentMethod;
import br.com.checkout.bff.client.product.CatalogProduct;
import br.com.checkout.bff.domain.PaymentMethodType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Simula os dois serviços externos (produtos e meios de pagamento) dentro do próprio projeto.
 * O BFF NÃO lê estas listas direto: consome por HTTP, pelos clientes {@code HttpProductClient}
 * e {@code HttpPaymentMethodClient}, como faria com os serviços reais.
 */
@RestController
@RequestMapping("/mock")
@ConditionalOnProperty(name = "checkout.mock-services.enabled", havingValue = "true", matchIfMissing = true)
public class MockExternalServicesController {

    private static final List<CatalogProduct> PRODUCTS = List.of(
            new CatalogProduct("basico", "Plano Básico", "Assinatura mensal com catálogo essencial",
                    new BigDecimal("19.90"), "BRL"),
            new CatalogProduct("premium", "Plano Premium", "Assinatura mensal com catálogo completo",
                    new BigDecimal("39.90"), "BRL"),
            new CatalogProduct("familia", "Plano Família", "Assinatura mensal com catálogo completo para até 5 perfis",
                    new BigDecimal("59.90"), "BRL")
    );

    private static final List<AvailablePaymentMethod> PAYMENT_METHODS = List.of(
            new AvailablePaymentMethod("credit_card", "Cartão de Crédito", PaymentMethodType.CREDIT_CARD),
            new AvailablePaymentMethod("debit_card", "Cartão de Débito", PaymentMethodType.DEBIT_CARD),
            new AvailablePaymentMethod("pix", "Pix", PaymentMethodType.PIX)
    );

    @GetMapping("/products")
    public List<CatalogProduct> products() {
        return PRODUCTS;
    }

    @GetMapping("/payment-methods")
    public List<AvailablePaymentMethod> paymentMethods() {
        return PAYMENT_METHODS;
    }
}
