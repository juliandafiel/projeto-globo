package br.com.checkout.bff.client;

import br.com.checkout.bff.domain.BusinessException;
import br.com.checkout.bff.domain.ErrorCode;

/** Serviço externo (produtos ou meios de pagamento) fora do ar ou respondendo errado. */
public class ExternalServiceException extends BusinessException {

    public ExternalServiceException(String service, Throwable cause) {
        super(ErrorCode.SERVICE_UNAVAILABLE, "Falha ao consultar o serviço de " + service + ": " + cause.getMessage());
        initCause(cause);
    }
}
