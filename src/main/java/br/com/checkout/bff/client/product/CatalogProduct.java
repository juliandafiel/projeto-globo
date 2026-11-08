package br.com.checkout.bff.client.product;

import java.io.Serializable;
import java.math.BigDecimal;

/** Produto como o serviço de produtos devolve. */
public record CatalogProduct(
        String slug,
        String name,
        String description,
        BigDecimal price,
        String currency
) implements Serializable {
}
