package br.com.checkout.bff.client;

import br.com.checkout.bff.client.paymentmethod.AvailablePaymentMethod;
import br.com.checkout.bff.client.paymentmethod.PaymentMethodClientImpl;
import br.com.checkout.bff.client.product.CatalogProduct;
import br.com.checkout.bff.client.product.ProductClientImpl;
import br.com.checkout.bff.domain.ErrorCode;
import br.com.checkout.bff.domain.PaymentMethodType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Clientes HTTP dos serviços externos: leitura do JSON e tradução de falhas. */
class HttpClientsTest {

    @Test
    void produtosLeOCatalogo() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://produtos");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://produtos/products")).andRespond(withSuccess("""
                [{"slug":"premium","name":"Plano Premium","description":"Completo","price":39.90,"currency":"BRL"}]
                """, MediaType.APPLICATION_JSON));

        List<CatalogProduct> products = new ProductClientImpl(builder.build()).findAll();

        assertThat(products).hasSize(1);
        assertThat(products.get(0).slug()).isEqualTo("premium");
        assertThat(products.get(0).price()).isEqualByComparingTo("39.90");
        server.verify();
    }

    @Test
    void produtosForaDoArViraServicoIndisponivel() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://produtos");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://produtos/products")).andRespond(withServerError());

        assertThatThrownBy(() -> new ProductClientImpl(builder.build()).findAll())
                .isInstanceOf(ExternalServiceException.class)
                .extracting(e -> ((ExternalServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
    }

    @Test
    void meiosDePagamentoLeALista() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://meios");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://meios/payment-methods")).andRespond(withSuccess("""
                [{"code":"pix","name":"Pix","type":"PIX"},
                 {"code":"credit_card","name":"Cartão de Crédito","type":"CREDIT_CARD"}]
                """, MediaType.APPLICATION_JSON));

        List<AvailablePaymentMethod> methods = new PaymentMethodClientImpl(builder.build()).findAll();

        assertThat(methods).extracting(AvailablePaymentMethod::type)
                .containsExactly(PaymentMethodType.PIX, PaymentMethodType.CREDIT_CARD);
        server.verify();
    }

    @Test
    void meiosForaDoArViraServicoIndisponivel() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://meios");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://meios/payment-methods")).andRespond(withServerError());

        assertThatThrownBy(() -> new PaymentMethodClientImpl(builder.build()).findAll())
                .isInstanceOf(ExternalServiceException.class);
    }
}
