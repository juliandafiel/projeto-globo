package br.com.checkout.bff.api.error;

/**
 * Objeto único de erro para o frontend. Toda falha sai neste formato, sempre com as mesmas
 * chaves ({@code cartId} e {@code secondaryButton} vêm null quando não se aplicam).
 */
public record ErrorResponse(
        DisplayType displayType,
        String errorCode,
        String cartId,
        FrontendError frontendError
) {
    public enum DisplayType {
        /** Erro de tela, com ações. */
        BLOCK,
        /** Aviso que não encerra o fluxo. */
        INTERVENTION
    }

    public record FrontendError(String code, String title, String description, Button primaryButton, Button secondaryButton) {
    }

    public record Button(String label, String url) {
    }
}
