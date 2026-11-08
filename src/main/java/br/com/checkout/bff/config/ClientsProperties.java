package br.com.checkout.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Endereços e timeouts dos serviços externos consumidos pelo BFF. */
@ConfigurationProperties("clients")
public record ClientsProperties(Service products, Service paymentMethods) {

    public record Service(String baseUrl, Duration connectTimeout, Duration readTimeout) {
    }
}
