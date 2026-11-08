package br.com.checkout.bff.domain;

/**
 * Falhas de negócio que o frontend precisa exibir. A forma de apresentar cada uma
 * (HTTP, displayType, textos e botões) fica no catálogo da camada de API.
 */
public enum ErrorCode {
    INVALID_REQUEST,
    PRODUCT_NOT_FOUND,
    CART_NOT_FOUND,
    PAYMENT_METHOD_NOT_ALLOWED,
    INVALID_PAYMENT_DATA,
    PAYMENT_DECLINED,
    CART_ALREADY_PAID,
    SERVICE_UNAVAILABLE,
    UNEXPECTED_ERROR
}
