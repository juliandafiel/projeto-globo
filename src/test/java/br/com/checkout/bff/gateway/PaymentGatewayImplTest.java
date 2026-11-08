package br.com.checkout.bff.gateway;

import br.com.checkout.bff.domain.PaymentMethodType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentGatewayImplTest {

    final PaymentGatewayImpl gateway = new PaymentGatewayImpl();
    final UUID cartId = UUID.fromString("f1d4c35a-8634-43ed-80e6-4d13c4271dbc");

    private ChargeRequest card(PaymentMethodType type, String number) {
        return new ChargeRequest(cartId, type, new BigDecimal("39.90"), "BRL",
                new CardData("Ana", number, "12/28", "123"));
    }

    @Test
    void cartaoTerminadoEm0002ERecusado() {
        ChargeResult r = gateway.charge(card(PaymentMethodType.CREDIT_CARD, "4000000000000002"));
        assertThat(r.approved()).isFalse();
        assertThat(r.declineReason()).isEqualTo("CARD_DECLINED");
        assertThat(r.lastDigits()).isEqualTo("0002");
    }

    @Test
    void debitoTerminadoEm0002TambemERecusado() {
        assertThat(gateway.charge(card(PaymentMethodType.DEBIT_CARD, "5555666677770002")).approved()).isFalse();
    }

    @Test
    void outroCartaoEAprovadoSoComOsUltimosDigitos() {
        ChargeResult r = gateway.charge(card(PaymentMethodType.CREDIT_CARD, "4111111111114242"));
        assertThat(r.approved()).isTrue();
        assertThat(r.lastDigits()).isEqualTo("4242");
        assertThat(r.pixCode()).isNull();
    }

    @Test
    void pixGeraBrCodeComValorETxidECrcValido() {
        ChargeResult r = gateway.charge(new ChargeRequest(cartId, PaymentMethodType.PIX, new BigDecimal("39.9"), "BRL", null));

        assertThat(r.approved()).isTrue();
        assertThat(r.lastDigits()).isNull();
        String code = r.pixCode();
        assertThat(code).startsWith("000201").contains("br.gov.bcb.pix").contains("540539.90").contains("5802BR");
        assertThat(code).contains("0525" + "f1d4c35a863443ed80e64d13c");

        // CRC confere: recalcular sobre tudo até "6304" dá os 4 últimos caracteres
        String withoutCrc = code.substring(0, code.length() - 4);
        assertThat(withoutCrc).endsWith("6304");
        assertThat(PaymentGatewayImpl.pixCode(
                new ChargeRequest(cartId, PaymentMethodType.PIX, new BigDecimal("39.90"), "BRL", null))).isEqualTo(code);
    }
}
