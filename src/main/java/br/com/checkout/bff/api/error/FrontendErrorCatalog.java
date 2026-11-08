package br.com.checkout.bff.api.error;

import br.com.checkout.bff.api.error.ErrorResponse.Button;
import br.com.checkout.bff.api.error.ErrorResponse.DisplayType;
import br.com.checkout.bff.api.error.ErrorResponse.FrontendError;
import br.com.checkout.bff.domain.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.EnumMap;
import java.util.Map;

/**
 * Como cada falha aparece para o usuário: HTTP, tipo de exibição, textos e botões.
 * Concentrado aqui para o contrato não variar entre endpoints.
 * {@code RETRY} no url do botão = o frontend repete a última ação.
 */
public final class FrontendErrorCatalog {

    public static final String RETRY = "RETRY";
    private static final String PAYMENT_SCREEN = "/checkout/pagamento";
    private static final String PLANS_SCREEN = "/planos";

    public record Entry(HttpStatus status, DisplayType displayType, FrontendError frontendError) {
    }

    private static final Map<ErrorCode, Entry> ENTRIES = new EnumMap<>(ErrorCode.class);

    static {
        put(ErrorCode.PAYMENT_DECLINED, HttpStatus.UNPROCESSABLE_CONTENT, DisplayType.BLOCK, "P-1001",
                "Pagamento não aprovado",
                "Não foi possível concluir o pagamento. Tente novamente ou escolha outro meio.",
                new Button("Tentar novamente", RETRY),
                new Button("Trocar meio de pagamento", PAYMENT_SCREEN));

        put(ErrorCode.PAYMENT_METHOD_NOT_ALLOWED, HttpStatus.UNPROCESSABLE_CONTENT, DisplayType.INTERVENTION, "P-1002",
                "Meio de pagamento indisponível",
                "Esse meio de pagamento não está disponível para esta compra. Escolha um dos meios oferecidos.",
                new Button("Escolher outro meio", PAYMENT_SCREEN), null);

        put(ErrorCode.INVALID_PAYMENT_DATA, HttpStatus.BAD_REQUEST, DisplayType.INTERVENTION, "P-1003",
                "Confira os dados do pagamento",
                "Algum dado do pagamento está incompleto ou inválido. Revise e tente de novo.",
                new Button("Revisar dados", PAYMENT_SCREEN), null);

        put(ErrorCode.CART_ALREADY_PAID, HttpStatus.CONFLICT, DisplayType.BLOCK, "P-1004",
                "Pagamento já confirmado",
                "Esta compra já foi paga e a assinatura está ativa. Nenhuma nova cobrança foi feita.",
                new Button("Ir para a minha assinatura", "/minha-assinatura"), null);

        put(ErrorCode.PRODUCT_NOT_FOUND, HttpStatus.UNPROCESSABLE_CONTENT, DisplayType.BLOCK, "P-2001",
                "Plano indisponível",
                "Um dos planos escolhidos não está disponível. Escolha outro plano para continuar.",
                new Button("Ver planos", PLANS_SCREEN), null);

        put(ErrorCode.CART_NOT_FOUND, HttpStatus.NOT_FOUND, DisplayType.BLOCK, "P-3001",
                "Carrinho não encontrado",
                "Não encontramos esta compra. Escolha o plano de novo para recomeçar.",
                new Button("Escolher plano", PLANS_SCREEN), null);

        put(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST, DisplayType.BLOCK, "P-0001",
                "Não foi possível continuar",
                "Faltaram informações para concluir esta etapa. Tente novamente.",
                new Button("Tentar novamente", RETRY),
                new Button("Ver planos", PLANS_SCREEN));

        put(ErrorCode.SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, DisplayType.BLOCK, "P-9001",
                "Serviço indisponível",
                "Não conseguimos carregar as informações agora. Tente novamente em instantes.",
                new Button("Tentar novamente", RETRY), null);

        put(ErrorCode.UNEXPECTED_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, DisplayType.BLOCK, "P-9999",
                "Algo deu errado",
                "Tivemos um problema inesperado. Nenhuma cobrança foi confirmada. Tente novamente.",
                new Button("Tentar novamente", RETRY), null);

        for (ErrorCode code : ErrorCode.values()) {
            if (!ENTRIES.containsKey(code)) {
                throw new IllegalStateException("ErrorCode sem entrada no catálogo: " + code);
            }
        }
    }

    private FrontendErrorCatalog() {
    }

    private static void put(ErrorCode code, HttpStatus status, DisplayType type, String frontCode, String title,
                            String description, Button primary, Button secondary) {
        ENTRIES.put(code, new Entry(status, type, new FrontendError(frontCode, title, description, primary, secondary)));
    }

    public static Entry of(ErrorCode code) {
        return ENTRIES.get(code);
    }
}
