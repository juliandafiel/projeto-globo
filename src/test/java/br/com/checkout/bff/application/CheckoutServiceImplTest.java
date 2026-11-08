package br.com.checkout.bff.application;

import br.com.checkout.bff.domain.BusinessException;
import br.com.checkout.bff.domain.ErrorCode;
import br.com.checkout.bff.domain.PaymentDeclinedException;
import br.com.checkout.bff.domain.PaymentMethodType;
import br.com.checkout.bff.events.PaymentApprovedEvent;
import br.com.checkout.bff.gateway.ChargeRequest;
import br.com.checkout.bff.gateway.ChargeResult;
import br.com.checkout.bff.gateway.PaymentGatewayI;
import br.com.checkout.bff.persistence.Cart;
import br.com.checkout.bff.persistence.CartItem;
import br.com.checkout.bff.persistence.CartPaymentMethod;
import br.com.checkout.bff.persistence.CartRepositoryI;
import br.com.checkout.bff.persistence.CartStatus;
import br.com.checkout.bff.persistence.Payment;
import br.com.checkout.bff.persistence.PaymentRepositoryI;
import br.com.checkout.bff.persistence.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceImplTest {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-14T16:00:00.123Z"), ZoneOffset.UTC);
    static final CardInput CARD = new CardInput("Ana Souza", "4111111111114242", "12/28", "123");

    @Mock
    CartRepositoryI cartRepository;
    @Mock
    PaymentRepositoryI paymentRepository;
    @Mock
    PaymentGatewayI gateway;
    @Mock
    ApplicationEventPublisher events;

    CheckoutServiceImpl service;
    Cart cart;

    @BeforeEach
    void setUp() {
        service = new CheckoutServiceImpl(cartRepository, paymentRepository, gateway, new CardValidator(CLOCK), events, CLOCK);
        cart = new Cart(UUID.randomUUID(),
                List.of(new CartItem(0, "premium", "Plano Premium", "desc", new BigDecimal("39.90"), "BRL")),
                List.of(new CartPaymentMethod("credit_card", "Cartão de Crédito", PaymentMethodType.CREDIT_CARD),
                        new CartPaymentMethod("pix", "Pix", PaymentMethodType.PIX)),
                "BRL", CLOCK.instant());
    }

    private void cartExists() {
        when(cartRepository.findByIdForUpdate(cart.getId())).thenReturn(Optional.of(cart));
    }

    private void paymentSaved() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private String id() {
        return cart.getId().toString();
    }

    @Test
    void aprovaCartaoMarcaPagoEPublicaEvento() {
        cartExists();
        paymentSaved();
        when(gateway.charge(any())).thenReturn(ChargeResult.approvedCard("4242"));

        CheckoutResult result = service.checkout(new CheckoutCommand(id(), "Cartão de Crédito", CARD));

        assertThat(result.cart().getStatus()).isEqualTo(CartStatus.PAID);
        assertThat(result.cart().getPaidAt()).isEqualTo(Instant.parse("2026-10-14T16:00:00Z"));
        assertThat(result.payment().getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.payment().getLastDigits()).isEqualTo("4242");
        assertThat(result.payment().getAmount()).isEqualByComparingTo("39.90");

        ArgumentCaptor<ChargeRequest> charge = ArgumentCaptor.forClass(ChargeRequest.class);
        verify(gateway).charge(charge.capture());
        assertThat(charge.getValue().type()).isEqualTo(PaymentMethodType.CREDIT_CARD);
        assertThat(charge.getValue().amount()).isEqualByComparingTo("39.90");

        ArgumentCaptor<PaymentApprovedEvent> event = ArgumentCaptor.forClass(PaymentApprovedEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().cartId()).isEqualTo(cart.getId());
        assertThat(event.getValue().productSlugs()).containsExactly("premium");
    }

    @Test
    void nomeDoMeioIgnoraMaiusculasEEspacos() {
        cartExists();
        paymentSaved();
        when(gateway.charge(any())).thenReturn(ChargeResult.approvedPix("000201..."));

        CheckoutResult result = service.checkout(new CheckoutCommand(id(), "  PIX ", null));

        assertThat(result.payment().getPixCode()).isEqualTo("000201...");
        assertThat(result.payment().getMethodName()).isEqualTo("Pix");
    }

    @Test
    void recusaGravaATentativaMantemCarrinhoAbertoENaoPublica() {
        cartExists();
        paymentSaved();
        when(gateway.charge(any())).thenReturn(ChargeResult.declined("0002", "CARD_DECLINED"));

        assertThatThrownBy(() -> service.checkout(new CheckoutCommand(id(), "Cartão de Crédito", CARD)))
                .isInstanceOf(PaymentDeclinedException.class)
                .extracting(e -> ((BusinessException) e).getCartId()).isEqualTo(id());

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(saved.getValue().getDeclineReason()).isEqualTo("CARD_DECLINED");
        assertThat(cart.getStatus()).isEqualTo(CartStatus.OPEN);
        verifyNoInteractions(events);
    }

    @Test
    void carrinhoInexistente() {
        UUID other = UUID.randomUUID();
        when(cartRepository.findByIdForUpdate(other)).thenReturn(Optional.empty());

        assertCode(() -> service.checkout(new CheckoutCommand(other.toString(), "Pix", null)), ErrorCode.CART_NOT_FOUND);
        verifyNoInteractions(gateway);
    }

    @Test
    void cartIdQueNaoEUuid() {
        assertCode(() -> service.checkout(new CheckoutCommand("abc", "Pix", null)), ErrorCode.CART_NOT_FOUND);
        verifyNoInteractions(cartRepository);
    }

    @Test
    void carrinhoJaPagoNaoCobraDeNovo() {
        cart.markPaid(CLOCK.instant());
        cartExists();

        assertCode(() -> service.checkout(new CheckoutCommand(id(), "Pix", null)), ErrorCode.CART_ALREADY_PAID);
        verifyNoInteractions(gateway);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void meioNaoOferecidoAoCarrinho() {
        cartExists();
        assertCode(() -> service.checkout(new CheckoutCommand(id(), "Cartão de Débito", CARD)),
                ErrorCode.PAYMENT_METHOD_NOT_ALLOWED);
        verifyNoInteractions(gateway);
    }

    @Test
    void pixNaoAceitaDadosDeCartao() {
        cartExists();
        assertCode(() -> service.checkout(new CheckoutCommand(id(), "Pix", CARD)), ErrorCode.INVALID_PAYMENT_DATA);
        verifyNoInteractions(gateway);
    }

    @Test
    void cartaoInvalidoNaoChegaAoGateway() {
        cartExists();
        CardInput semCvv = new CardInput("Ana", "4111111111114242", "12/28", "");
        assertCode(() -> service.checkout(new CheckoutCommand(id(), "Cartão de Crédito", semCvv)),
                ErrorCode.INVALID_PAYMENT_DATA);
        verifyNoInteractions(gateway);
    }

    private static void assertCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
