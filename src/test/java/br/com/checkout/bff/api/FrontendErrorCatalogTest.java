package br.com.checkout.bff.api;

import br.com.checkout.bff.api.error.FrontendErrorCatalog;
import br.com.checkout.bff.domain.ErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Garante que todo código de erro tem apresentação completa e o status combinado. */
class FrontendErrorCatalogTest {

    /** Falhas de infraestrutura (não causadas pelo pedido) saem 5xx, com o mesmo objeto de erro. */
    static final Set<ErrorCode> INFRA = EnumSet.of(ErrorCode.SERVICE_UNAVAILABLE, ErrorCode.UNEXPECTED_ERROR);

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void todoCodigoTemTituloDescricaoEBotaoPrincipal(ErrorCode code) {
        FrontendErrorCatalog.Entry entry = FrontendErrorCatalog.of(code);

        assertThat(entry).isNotNull();
        assertThat(entry.displayType()).isNotNull();
        assertThat(entry.frontendError().code()).matches("P-\\d{4}");
        assertThat(entry.frontendError().title()).isNotBlank();
        assertThat(entry.frontendError().description()).isNotBlank();
        assertThat(entry.frontendError().primaryButton()).isNotNull();
        assertThat(entry.frontendError().primaryButton().label()).isNotBlank();
        assertThat(entry.frontendError().primaryButton().url()).isNotBlank();

        if (INFRA.contains(code)) {
            assertThat(entry.status().is5xxServerError()).isTrue();
        } else {
            assertThat(entry.status().is4xxClientError()).isTrue();
        }
    }
}
