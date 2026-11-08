package br.com.checkout.bff.persistence;

import br.com.checkout.bff.domain.PaymentMethodType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** Meio de pagamento oferecido no pré-checkout deste carrinho. */
@Embeddable
public class CartPaymentMethod {

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethodType type;

    protected CartPaymentMethod() {
    }

    public CartPaymentMethod(String code, String name, PaymentMethodType type) {
        this.code = code;
        this.name = name;
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public PaymentMethodType getType() {
        return type;
    }
}
