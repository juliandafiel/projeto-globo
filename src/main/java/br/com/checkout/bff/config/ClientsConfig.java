package br.com.checkout.bff.config;

import br.com.checkout.bff.client.paymentmethod.PaymentMethodClientImpl;
import br.com.checkout.bff.client.paymentmethod.PaymentMethodClientI;
import br.com.checkout.bff.client.product.ProductClientImpl;
import br.com.checkout.bff.client.product.ProductClientI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;

@Configuration
public class ClientsConfig {

    @Bean
    ProductClientI productClient(RestClient.Builder builder, ClientsProperties props) {
        return new ProductClientImpl(restClient(builder, props.products()));
    }

    @Bean
    PaymentMethodClientI paymentMethodClient(RestClient.Builder builder, ClientsProperties props) {
        return new PaymentMethodClientImpl(restClient(builder, props.paymentMethods()));
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    private static RestClient restClient(RestClient.Builder builder, ClientsProperties.Service service) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(service.connectTimeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(service.readTimeout());
        return builder.clone().baseUrl(service.baseUrl()).requestFactory(factory).build();
    }
}
