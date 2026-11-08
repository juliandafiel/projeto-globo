package br.com.checkout.bff.client.product;

import br.com.checkout.bff.client.ExternalServiceException;
import br.com.checkout.bff.config.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/** Cliente HTTP do serviço de produtos. O catálogo muda pouco, então fica em cache. */
public class HttpProductClient implements ProductClient {

    private static final ParameterizedTypeReference<List<CatalogProduct>> LIST = new ParameterizedTypeReference<>() { };

    private final RestClient restClient;

    public HttpProductClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @Cacheable(CacheConfig.PRODUCTS)
    public List<CatalogProduct> findAll() {
        try {
            List<CatalogProduct> body = restClient.get().uri("/products").retrieve().body(LIST);
            return body == null ? List.of() : List.copyOf(body);
        } catch (RestClientException e) {
            throw new ExternalServiceException("produtos", e);
        }
    }
}
