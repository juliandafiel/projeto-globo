package br.com.checkout.bff.domain;

/** Falha de negócio. A mensagem é só para log; o texto do usuário vem do catálogo de erros. */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String cartId;

    public BusinessException(ErrorCode errorCode, String cartId, String message) {
        super(message);
        this.errorCode = errorCode;
        this.cartId = cartId;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, null, message);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getCartId() {
        return cartId;
    }
}
