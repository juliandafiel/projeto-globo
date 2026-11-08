package br.com.checkout.bff.api;

import br.com.checkout.bff.api.dto.CheckoutRequest;
import br.com.checkout.bff.api.dto.CheckoutResponse;
import br.com.checkout.bff.api.dto.PreCheckoutRequest;
import br.com.checkout.bff.api.dto.PreCheckoutResponse;
import br.com.checkout.bff.application.CardInput;
import br.com.checkout.bff.application.CheckoutCommand;
import br.com.checkout.bff.application.CheckoutResult;
import br.com.checkout.bff.application.CheckoutServiceI;
import br.com.checkout.bff.application.PreCheckoutServiceI;
import br.com.checkout.bff.persistence.Cart;
import br.com.checkout.bff.persistence.Payment;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints do BFF. Só traduz HTTP ⇄ casos de uso; a regra fica na camada de aplicação. */
@RestController
@RequestMapping("/operations")
public class OperationsController {

    private final PreCheckoutServiceI preCheckoutService;
    private final CheckoutServiceI checkoutService;

    public OperationsController(PreCheckoutServiceI preCheckoutService, CheckoutServiceI checkoutService) {
        this.preCheckoutService = preCheckoutService;
        this.checkoutService = checkoutService;
    }

    @PostMapping("/pre-checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public PreCheckoutResponse preCheckout(@Valid @RequestBody PreCheckoutRequest request) {
        Cart cart = preCheckoutService.createCart(
                request.products().stream().map(PreCheckoutRequest.ProductRef::slug).toList());
        return new PreCheckoutResponse(
                cart.getId().toString(),
                cart.getItems().stream()
                        .map(i -> new PreCheckoutResponse.Product(i.getSlug(), i.getName(), i.getDescription(),
                                i.getPrice(), i.getCurrency()))
                        .toList(),
                cart.getPaymentMethods().stream()
                        .map(m -> new PreCheckoutResponse.PaymentMethod(m.getName()))
                        .toList());
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        CheckoutRequest.Card card = request.paymentMethod().card();
        CardInput cardInput = card == null ? null
                : new CardInput(card.holderName(), card.number(), card.expiry(), card.cvv());

        CheckoutResult result = checkoutService.checkout(
                new CheckoutCommand(request.cartId(), request.paymentMethod().name(), cardInput));

        Cart cart = result.cart();
        Payment payment = result.payment();
        return new CheckoutResponse(
                cart.getId().toString(),
                CheckoutResponse.APPROVED,
                cart.getPaidAt(),
                new CheckoutResponse.Payment(payment.getMethodName(), payment.getLastDigits(), payment.getPixCode(),
                        payment.getAmount(), payment.getCurrency()),
                cart.getItems().stream()
                        .map(i -> new CheckoutResponse.Product(i.getSlug(), i.getName(), i.getPrice()))
                        .toList());
    }
}
