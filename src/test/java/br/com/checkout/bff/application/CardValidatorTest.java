package br.com.checkout.bff.application;

import br.com.checkout.bff.domain.BusinessException;
import br.com.checkout.bff.domain.ErrorCode;
import br.com.checkout.bff.gateway.CardData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardValidatorTest {

    final CardValidator validator =
            new CardValidator(Clock.fixed(Instant.parse("2026-10-14T16:00:00Z"), ZoneOffset.UTC));

    @Test
    void aceitaCartaoValidoENormalizaONumero() {
        CardData card = validator.validate("c1", new CardInput(" Ana Souza ", "4111 1111-1111 4242", "12/28", "123"));
        assertThat(card.number()).isEqualTo("4111111111114242");
        assertThat(card.holderName()).isEqualTo("Ana Souza");
        assertThat(card.lastDigits()).isEqualTo("4242");
    }

    @Test
    void aceitaCartaoQueVenceNoMesCorrente() {
        assertThat(validator.validate("c1", new CardInput("Ana", "4111111111114242", "10/26", "123"))).isNotNull();
    }

    @Test
    void toStringNaoExpoeNumeroNemCvv() {
        CardData card = validator.validate("c1", new CardInput("Ana", "4111111111114242", "12/2028", "987"));
        assertThat(card.toString()).doesNotContain("4111111111114242").doesNotContain("987").contains("4242");
        assertThat(new CardInput("Ana", "4111111111114242", "12/28", "987").toString())
                .doesNotContain("4111111111114242").doesNotContain("987");
    }

    @ParameterizedTest
    @CsvSource(value = {
            "'', 4111111111114242, 12/28, 123",      // sem titular
            "Ana, 4111, 12/28, 123",                  // número curto
            "Ana, 4111abcd11114242, 12/28, 123",      // letras
            "Ana, 4111111111114242, 13/28, 123",      // mês inválido
            "Ana, 4111111111114242, 1228, 123",       // formato da validade
            "Ana, 4111111111114242, 09/26, 123",      // vencido
            "Ana, 4111111111114242, 12/28, 12",       // cvv curto
            "Ana, 4111111111114242, 12/28, NULL",     // sem cvv
    }, nullValues = "NULL")
    void recusaDadosInvalidos(String holder, String number, String expiry, String cvv) {
        assertThatThrownBy(() -> validator.validate("c1", new CardInput(holder, number, expiry, cvv)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PAYMENT_DATA);
    }

    @Test
    void recusaCartaoAusente() {
        assertThatThrownBy(() -> validator.validate("c1", null))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PAYMENT_DATA);
    }
}
