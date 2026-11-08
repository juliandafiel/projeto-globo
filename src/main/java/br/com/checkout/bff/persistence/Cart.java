package br.com.checkout.bff.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "carts")
public class Cart {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Version
    private long version;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "cart_id", nullable = false)
    private List<CartItem> items = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cart_payment_methods", joinColumns = @JoinColumn(name = "cart_id"))
    @OrderColumn(name = "position")
    private List<CartPaymentMethod> paymentMethods = new ArrayList<>();

    protected Cart() {
    }

    public Cart(UUID id, List<CartItem> items, List<CartPaymentMethod> paymentMethods, String currency, Instant createdAt) {
        this.id = id;
        this.status = CartStatus.OPEN;
        this.items = new ArrayList<>(items);
        this.paymentMethods = new ArrayList<>(paymentMethods);
        this.currency = currency;
        this.createdAt = createdAt;
        this.totalAmount = items.stream().map(CartItem::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isPaid() {
        return status == CartStatus.PAID;
    }

    public void markPaid(Instant when) {
        if (isPaid()) {
            throw new IllegalStateException("Carrinho " + id + " já está pago");
        }
        this.status = CartStatus.PAID;
        this.paidAt = when;
    }

    /** Meio de pagamento oferecido a este carrinho com esse nome (sem diferenciar maiúsculas). */
    public Optional<CartPaymentMethod> findPaymentMethod(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String wanted = name.trim();
        return paymentMethods.stream().filter(m -> m.getName().equalsIgnoreCase(wanted)).findFirst();
    }

    public UUID getId() {
        return id;
    }

    public CartStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public List<CartItem> getItems() {
        return items.stream().sorted(Comparator.comparingInt(CartItem::getPosition)).toList();
    }

    public List<CartPaymentMethod> getPaymentMethods() {
        return List.copyOf(paymentMethods);
    }
}
