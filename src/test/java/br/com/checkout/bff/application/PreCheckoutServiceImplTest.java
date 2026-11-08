package br.com.checkout.bff.application;

import br.com.checkout.bff.TestData;
import br.com.checkout.bff.client.ExternalServiceException;
import br.com.checkout.bff.client.paymentmethod.PaymentMethodClientI;
import br.com.checkout.bff.client.product.ProductClientI;
import br.com.checkout.bff.domain.BusinessException;
import br.com.checkout.bff.domain.ErrorCode;
import br.com.checkout.bff.persistence.Cart;
import br.com.checkout.bff.persistence.CartItem;
import br.com.checkout.bff.persistence.CartPaymentMethod;
import br.com.checkout.bff.persistence.CartRepositoryI;
import br.com.checkout.bff.persistence.CartStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreCheckoutServiceImplTest {

    @Mock
    ProductClientI productClient;
    @Mock
    PaymentMethodClientI paymentMethodClient;
    @Mock
    CartRepositoryI cartRepository;

    PreCheckoutServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-14T16:00:00Z"), ZoneOffset.UTC);
        service = new PreCheckoutServiceImpl(productClient, paymentMethodClient, cartRepository, clock);
    }

    @Test
    void criaCarrinhoComCopiaDosProdutosEDosMeios() {
        when(productClient.findAll()).thenReturn(TestData.PRODUCTS);
        when(paymentMethodClient.findAll()).thenReturn(TestData.PAYMENT_METHODS);
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart cart = service.createCart(List.of("familia", "premium"));

        assertThat(cart.getId()).isNotNull();
        assertThat(cart.getStatus()).isEqualTo(CartStatus.OPEN);
        assertThat(cart.getItems()).extracting(CartItem::getSlug).containsExactly("familia", "premium");
        assertThat(cart.getTotalAmount()).isEqualByComparingTo("99.80");
        assertThat(cart.getCurrency()).isEqualTo("BRL");
        assertThat(cart.getPaymentMethods()).extracting(CartPaymentMethod::getName)
                .containsExactly("Cartão de Crédito", "Cartão de Débito", "Pix");
    }

    @Test
    void mesmoPlanoRepetidoEntraUmaVezSo() {
        when(productClient.findAll()).thenReturn(TestData.PRODUCTS);
        when(paymentMethodClient.findAll()).thenReturn(TestData.PAYMENT_METHODS);
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart cart = service.createCart(List.of("premium", " PREMIUM "));

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getTotalAmount()).isEqualByComparingTo("39.90");
    }

    @Test
    void produtoInexistenteNaoCriaCarrinho() {
        when(productClient.findAll()).thenReturn(TestData.PRODUCTS);

        assertThatThrownBy(() -> service.createCart(List.of("premium", "ultra")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void falhaNoServicoDeMeiosPropagaComoServicoIndisponivel() {
        when(productClient.findAll()).thenReturn(TestData.PRODUCTS);
        when(paymentMethodClient.findAll()).thenThrow(new ExternalServiceException("meios", new RuntimeException("x")));

        assertThatThrownBy(() -> service.createCart(List.of("premium")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void semMeiosDePagamentoNaoCriaCarrinho() {
        when(productClient.findAll()).thenReturn(TestData.PRODUCTS);
        when(paymentMethodClient.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.createCart(List.of("premium")))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
    }
}
