package br.com.checkout.bff.client.paymentmethod;

import br.com.checkout.bff.client.ExternalServiceException;
import br.com.checkout.bff.config.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/** Cliente HTTP do serviço de meios de pagamento, com cache. */
public class PaymentMethodClientImpl implements PaymentMethodClientI {

    private static final ParameterizedTypeReference<List<AvailablePaymentMethod>> LIST = new ParameterizedTypeReference<>() { };

    private final RestClient restClient;

    public PaymentMethodClientImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @Cacheable(CacheConfig.PAYMENT_METHODS)
    public List<AvailablePaymentMethod> findAll() {
        try {
            List<AvailablePaymentMethod> body = restClient.get().uri("/payment-methods").retrieve().body(LIST);
            return body == null ? List.of() : List.copyOf(body);
        } catch (RestClientException e) {
            throw new ExternalServiceException("meios de pagamento", e);
        }
    }
}
