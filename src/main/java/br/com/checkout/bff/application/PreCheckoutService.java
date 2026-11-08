package br.com.checkout.bff.application;

import br.com.checkout.bff.client.paymentmethod.AvailablePaymentMethod;
import br.com.checkout.bff.client.paymentmethod.PaymentMethodClient;
import br.com.checkout.bff.client.product.CatalogProduct;
import br.com.checkout.bff.client.product.ProductClient;
import br.com.checkout.bff.domain.BusinessException;
import br.com.checkout.bff.domain.ErrorCode;
import br.com.checkout.bff.persistence.Cart;
import br.com.checkout.bff.persistence.CartItem;
import br.com.checkout.bff.persistence.CartPaymentMethod;
import br.com.checkout.bff.persistence.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pré-checkout: consulta produtos e meios de pagamento nos serviços externos e grava um
 * carrinho com a cópia do que foi oferecido.
 */
@Service
public class PreCheckoutService {

    private final ProductClient productClient;
    private final PaymentMethodClient paymentMethodClient;
    private final CartRepository cartRepository;
    private final Clock clock;

    public PreCheckoutService(ProductClient productClient, PaymentMethodClient paymentMethodClient,
                              CartRepository cartRepository, Clock clock) {
        this.productClient = productClient;
        this.paymentMethodClient = paymentMethodClient;
        this.cartRepository = cartRepository;
        this.clock = clock;
    }

    @Transactional
    public Cart createCart(List<String> requestedSlugs) {
        // Assinatura não tem quantidade: o mesmo plano repetido conta uma vez só.
        Set<String> slugs = requestedSlugs.stream()
                .filter(Objects::nonNull)
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, CatalogProduct> catalog = productClient.findAll().stream()
                .collect(Collectors.toMap(CatalogProduct::slug, Function.identity(), (a, b) -> a));

        List<String> missing = slugs.stream().filter(s -> !catalog.containsKey(s)).toList();
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Produtos inexistentes: " + missing);
        }

        List<AvailablePaymentMethod> methods = paymentMethodClient.findAll();
        if (methods.isEmpty()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Nenhum meio de pagamento disponível");
        }

        List<CartItem> items = new ArrayList<>();
        for (String slug : slugs) {
            CatalogProduct p = catalog.get(slug);
            items.add(new CartItem(items.size(), p.slug(), p.name(), p.description(), p.price(), p.currency()));
        }
        Set<String> currencies = items.stream().map(CartItem::getCurrency).collect(Collectors.toSet());
        if (currencies.size() != 1) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Produtos com moedas diferentes: " + currencies);
        }

        List<CartPaymentMethod> offered = methods.stream()
                .map(m -> new CartPaymentMethod(m.code(), m.name(), m.type()))
                .toList();

        Cart cart = new Cart(UUID.randomUUID(), items, offered, currencies.iterator().next(), clock.instant());
        return cartRepository.save(cart);
    }
}
