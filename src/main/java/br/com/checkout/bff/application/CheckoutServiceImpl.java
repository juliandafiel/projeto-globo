package br.com.checkout.bff.application;

import br.com.checkout.bff.domain.BusinessException;
import br.com.checkout.bff.domain.ErrorCode;
import br.com.checkout.bff.domain.PaymentDeclinedException;
import br.com.checkout.bff.events.PaymentApprovedEvent;
import br.com.checkout.bff.gateway.CardData;
import br.com.checkout.bff.gateway.ChargeRequest;
import br.com.checkout.bff.gateway.ChargeResult;
import br.com.checkout.bff.gateway.PaymentGatewayI;
import br.com.checkout.bff.persistence.Cart;
import br.com.checkout.bff.persistence.CartItem;
import br.com.checkout.bff.persistence.CartPaymentMethod;
import br.com.checkout.bff.persistence.CartRepositoryI;
import br.com.checkout.bff.persistence.Payment;
import br.com.checkout.bff.persistence.PaymentRepositoryI;
import br.com.checkout.bff.persistence.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Regra do checkout:
 * <ol>
 *   <li>o carrinho precisa existir (criado no pré-checkout) e ainda não estar pago;</li>
 *   <li>o meio de pagamento precisa ser um dos oferecidos àquele carrinho;</li>
 *   <li>cartão exige dados válidos; Pix não aceita dados de cartão;</li>
 *   <li>a cobrança é simulada; recusa fica registrada e o carrinho continua aberto para nova tentativa;</li>
 *   <li>aprovação marca o carrinho como pago e publica o evento depois do commit.</li>
 * </ol>
 */
@Service
public class CheckoutServiceImpl implements CheckoutServiceI {

    private static final Logger log = LoggerFactory.getLogger(CheckoutServiceImpl.class);

    private final CartRepositoryI cartRepository;
    private final PaymentRepositoryI paymentRepository;
    private final PaymentGatewayI paymentGateway;
    private final CardValidator cardValidator;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public CheckoutServiceImpl(CartRepositoryI cartRepository, PaymentRepositoryI paymentRepository,
                           PaymentGatewayI paymentGateway, CardValidator cardValidator,
                           ApplicationEventPublisher events, Clock clock) {
        this.cartRepository = cartRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.cardValidator = cardValidator;
        this.events = events;
        this.clock = clock;
    }

    // A recusa precisa ficar gravada: por isso PaymentDeclinedException não desfaz a transação.
    @Override
    @Transactional(noRollbackFor = PaymentDeclinedException.class)
    public CheckoutResult checkout(CheckoutCommand command) {
        String cartIdText = command.cartId();
        UUID cartId = parseCartId(cartIdText);

        Cart cart = cartRepository.findByIdForUpdate(cartId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_NOT_FOUND, cartIdText, "Carrinho inexistente"));

        if (cart.isPaid()) {
            throw new BusinessException(ErrorCode.CART_ALREADY_PAID, cartIdText, "Carrinho já pago");
        }

        CartPaymentMethod method = cart.findPaymentMethod(command.paymentMethodName())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_METHOD_NOT_ALLOWED, cartIdText,
                        "Meio não oferecido a este carrinho: " + command.paymentMethodName()));

        CardData card = null;
        if (method.getType().usesCard()) {
            card = cardValidator.validate(cartIdText, command.card());
        } else if (command.card() != null) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_DATA, cartIdText, "Pix não recebe dados de cartão");
        }

        ChargeResult charge = paymentGateway.charge(
                new ChargeRequest(cart.getId(), method.getType(), cart.getTotalAmount(), cart.getCurrency(), card));

        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Payment payment = paymentRepository.save(new Payment(
                UUID.randomUUID(), cart.getId(), method.getName(), method.getType(),
                charge.approved() ? PaymentStatus.APPROVED : PaymentStatus.DECLINED,
                cart.getTotalAmount(), cart.getCurrency(), charge.lastDigits(), charge.pixCode(),
                charge.declineReason(), now));

        if (!charge.approved()) {
            log.info("Pagamento recusado: cart={} meio={} motivo={}", cart.getId(), method.getName(), charge.declineReason());
            throw new PaymentDeclinedException(cartIdText, charge.declineReason());
        }

        cart.markPaid(now);
        log.info("Pagamento aprovado: cart={} meio={} valor={}", cart.getId(), method.getName(), cart.getTotalAmount());

        events.publishEvent(new PaymentApprovedEvent(
                cart.getId(), payment.getId(), method.getName(), method.getType(),
                cart.getTotalAmount(), cart.getCurrency(), now,
                cart.getItems().stream().map(CartItem::getSlug).toList()));

        return new CheckoutResult(cart, payment);
    }

    private static UUID parseCartId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ErrorCode.CART_NOT_FOUND, raw, "cartId ausente");
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.CART_NOT_FOUND, raw, "cartId não é um UUID");
        }
    }
}
