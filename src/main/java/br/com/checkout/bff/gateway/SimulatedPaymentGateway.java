package br.com.checkout.bff.gateway;

import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;

/**
 * Cobrança simulada (sem gateway real).
 * <ul>
 *   <li>Cartão terminado em {@value #DECLINED_SUFFIX}: recusado.</li>
 *   <li>Qualquer outro cartão: aprovado.</li>
 *   <li>Pix: aprovado, com um código "copia e cola" no formato EMV do BR Code (fictício).</li>
 * </ul>
 */
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    public static final String DECLINED_SUFFIX = "0002";
    public static final String DECLINE_REASON = "CARD_DECLINED";

    private static final String MERCHANT_NAME = "CHECKOUT BFF";
    private static final String MERCHANT_CITY = "RIO DE JANEIRO";

    @Override
    public ChargeResult charge(ChargeRequest request) {
        if (request.type().usesCard()) {
            CardData card = request.card();
            if (card.number().endsWith(DECLINED_SUFFIX)) {
                return ChargeResult.declined(card.lastDigits(), DECLINE_REASON);
            }
            return ChargeResult.approvedCard(card.lastDigits());
        }
        return ChargeResult.approvedPix(pixCode(request));
    }

    /** Monta um BR Code estático (TLV + CRC16) com a chave e o txid derivados do carrinho. */
    static String pixCode(ChargeRequest request) {
        String txid = request.cartId().toString().replace("-", "").substring(0, 25);
        String amount = request.amount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String merchantAccount = tlv("00", "br.gov.bcb.pix") + tlv("01", request.cartId().toString());
        String payload = tlv("00", "01")
                + tlv("26", merchantAccount)
                + tlv("52", "0000")
                + tlv("53", "986")
                + tlv("54", amount)
                + tlv("58", "BR")
                + tlv("59", MERCHANT_NAME)
                + tlv("60", MERCHANT_CITY)
                + tlv("62", tlv("05", txid))
                + "6304";
        return payload + crc16(payload);
    }

    private static String tlv(String id, String value) {
        return id + String.format("%02d", value.length()) + value;
    }

    /** CRC16-CCITT (polinômio 0x1021, inicial 0xFFFF), como pede o padrão do BR Code. */
    private static String crc16(String payload) {
        int crc = 0xFFFF;
        for (byte b : payload.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }
}
