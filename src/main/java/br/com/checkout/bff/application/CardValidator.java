package br.com.checkout.bff.application;

import br.com.checkout.bff.domain.BusinessException;
import br.com.checkout.bff.domain.ErrorCode;
import br.com.checkout.bff.gateway.CardData;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Valida o formato do cartão antes da cobrança. Não aplica Luhn: o desafio usa números de
 * exemplo (ex.: 4111111111114242) que não precisam ser válidos para a bandeira.
 */
@Component
public class CardValidator {

    private static final Pattern DIGITS = Pattern.compile("\\d{13,19}");
    private static final Pattern CVV = Pattern.compile("\\d{3,4}");
    private static final Pattern EXPIRY = Pattern.compile("(\\d{2})/(\\d{2}|\\d{4})");

    private final Clock clock;

    public CardValidator(Clock clock) {
        this.clock = clock;
    }

    public CardData validate(String cartId, CardInput card) {
        if (card == null) {
            throw invalid(cartId, "dados do cartão ausentes");
        }
        String holder = card.holderName() == null ? "" : card.holderName().trim();
        if (holder.isEmpty() || holder.length() > 100) {
            throw invalid(cartId, "nome do titular inválido");
        }
        String number = card.number() == null ? "" : card.number().replaceAll("[\\s-]", "");
        if (!DIGITS.matcher(number).matches()) {
            throw invalid(cartId, "número do cartão inválido");
        }
        if (card.cvv() == null || !CVV.matcher(card.cvv().trim()).matches()) {
            throw invalid(cartId, "CVV inválido");
        }
        YearMonth expiry = parseExpiry(cartId, card.expiry());
        if (expiry.isBefore(YearMonth.now(clock.withZone(ZoneOffset.UTC)))) {
            throw invalid(cartId, "cartão vencido");
        }
        return new CardData(holder, number, card.expiry().trim(), card.cvv().trim());
    }

    private YearMonth parseExpiry(String cartId, String raw) {
        Matcher m = raw == null ? null : EXPIRY.matcher(raw.trim());
        if (m == null || !m.matches()) {
            throw invalid(cartId, "validade inválida");
        }
        int month = Integer.parseInt(m.group(1));
        int year = Integer.parseInt(m.group(2));
        if (month < 1 || month > 12) {
            throw invalid(cartId, "mês da validade inválido");
        }
        return YearMonth.of(year < 100 ? 2000 + year : year, month);
    }

    private static BusinessException invalid(String cartId, String detail) {
        return new BusinessException(ErrorCode.INVALID_PAYMENT_DATA, cartId, "Cartão: " + detail);
    }
}
