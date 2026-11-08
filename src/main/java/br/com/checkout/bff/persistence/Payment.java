package br.com.checkout.bff.persistence;

import br.com.checkout.bff.domain.PaymentMethodType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Tentativa de pagamento de um carrinho. De propósito não tem campo para número completo
 * do cartão nem CVV: só os 4 últimos dígitos.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Column(name = "cart_id", nullable = false)
    private UUID cartId;

    @Column(name = "method_name", nullable = false)
    private String methodName;

    @Enumerated(EnumType.STRING)
    @Column(name = "method_type", nullable = false)
    private PaymentMethodType methodType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(name = "last_digits")
    private String lastDigits;

    @Column(name = "pix_code")
    private String pixCode;

    @Column(name = "decline_reason")
    private String declineReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Payment() {
    }

    public Payment(UUID id, UUID cartId, String methodName, PaymentMethodType methodType, PaymentStatus status,
                   BigDecimal amount, String currency, String lastDigits, String pixCode, String declineReason,
                   Instant createdAt) {
        this.id = id;
        this.cartId = cartId;
        this.methodName = methodName;
        this.methodType = methodType;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.lastDigits = lastDigits;
        this.pixCode = pixCode;
        this.declineReason = declineReason;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCartId() {
        return cartId;
    }

    public String getMethodName() {
        return methodName;
    }

    public PaymentMethodType getMethodType() {
        return methodType;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getLastDigits() {
        return lastDigits;
    }

    public String getPixCode() {
        return pixCode;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
