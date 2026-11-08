package br.com.checkout.bff.api.error;

import br.com.checkout.bff.domain.BusinessException;
import br.com.checkout.bff.domain.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Converte toda exceção no mesmo {@link ErrorResponse}. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> business(BusinessException e) {
        if (e.getErrorCode() == ErrorCode.SERVICE_UNAVAILABLE) {
            log.warn("{}", e.getMessage());
        } else {
            log.info("Falha de negócio {}: {}", e.getErrorCode(), e.getMessage());
        }
        return build(e.getErrorCode(), e.getCartId());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class,
            HttpMediaTypeNotSupportedException.class,
            HttpRequestMethodNotSupportedException.class,
            NoResourceFoundException.class
    })
    ResponseEntity<ErrorResponse> invalidRequest(Exception e) {
        log.info("Requisição inválida: {}", e.getClass().getSimpleName());
        return build(ErrorCode.INVALID_REQUEST, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e) {
        log.error("Erro inesperado", e);
        return build(ErrorCode.UNEXPECTED_ERROR, null);
    }

    public static ResponseEntity<ErrorResponse> build(ErrorCode code, String cartId) {
        FrontendErrorCatalog.Entry entry = FrontendErrorCatalog.of(code);
        return ResponseEntity.status(entry.status())
                .body(new ErrorResponse(entry.displayType(), code.name(), cartId, entry.frontendError()));
    }
}
