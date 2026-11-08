package br.com.checkout.bff.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Produto como estava no catálogo quando o carrinho foi montado. */
@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String currency;

    protected CartItem() {
    }

    public CartItem(int position, String slug, String name, String description, BigDecimal price, String currency) {
        this.position = position;
        this.slug = slug;
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
    }

    public int getPosition() {
        return position;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }
}
