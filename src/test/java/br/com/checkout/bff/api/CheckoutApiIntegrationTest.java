package br.com.checkout.bff.api;

import br.com.checkout.bff.TestData;
import br.com.checkout.bff.client.ExternalServiceException;
import br.com.checkout.bff.client.paymentmethod.PaymentMethodClientI;
import br.com.checkout.bff.client.product.ProductClientI;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo pela API (controller → regra → banco H2), com os serviços externos
 * mockados pela interface do BFF. Confere o contrato de sucesso e o de erro.
 */
@SpringBootTest
@ActiveProfiles("test")
class CheckoutApiIntegrationTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    ProductClientI productClient;

    @MockitoBean
    PaymentMethodClientI paymentMethodClient;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        when(productClient.findAll()).thenReturn(TestData.PRODUCTS);
        when(paymentMethodClient.findAll()).thenReturn(TestData.PAYMENT_METHODS);
    }

    // ---------- helpers ----------

    ResultActions postJson(String path, String body) throws Exception {
        return mvc.perform(post("/operations" + path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    String createCart(String... slugs) throws Exception {
        String products = String.join(",", java.util.Arrays.stream(slugs).map(s -> "{\"slug\":\"" + s + "\"}").toList());
        String body = postJson("/pre-checkout", "{\"products\":[" + products + "]}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.cartId");
    }

    static String cardPayment(String cartId, String method, String number) {
        return """
                {"cartId":"%s","paymentMethod":{"name":"%s","card":{
                  "holderName":"Ana Souza","number":"%s","expiry":"12/40","cvv":"123"}}}
                """.formatted(cartId, method, number);
    }

    static String pixPayment(String cartId) {
        return "{\"cartId\":\"" + cartId + "\",\"paymentMethod\":{\"name\":\"Pix\"}}";
    }

    /** Todo erro tem exatamente este formato. */
    static void assertErrorContract(ResultActions result, int http, String displayType, String errorCode, String frontCode)
            throws Exception {
        result.andExpect(status().is(http))
                .andExpect(jsonPath("$.displayType").value(displayType))
                .andExpect(jsonPath("$.errorCode").value(errorCode))
                .andExpect(jsonPath("$.cartId").hasJsonPath())
                .andExpect(jsonPath("$.frontendError.code").value(frontCode))
                .andExpect(jsonPath("$.frontendError.title").isNotEmpty())
                .andExpect(jsonPath("$.frontendError.description").isNotEmpty())
                .andExpect(jsonPath("$.frontendError.primaryButton.label").isNotEmpty())
                .andExpect(jsonPath("$.frontendError.primaryButton.url").isNotEmpty())
                .andExpect(jsonPath("$.frontendError.secondaryButton").hasJsonPath());
    }

    // ---------- pré-checkout ----------

    @Nested
    class PreCheckout {

        @Test
        void montaCarrinhoComProdutoEMeiosDoServico() throws Exception {
            postJson("/pre-checkout", "{\"products\":[{\"slug\":\"premium\"}]}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.cartId").isNotEmpty())
                    .andExpect(jsonPath("$.products.length()").value(1))
                    .andExpect(jsonPath("$.products[0].slug").value("premium"))
                    .andExpect(jsonPath("$.products[0].name").value("Plano Premium"))
                    .andExpect(jsonPath("$.products[0].description").value("Assinatura mensal com catálogo completo"))
                    .andExpect(jsonPath("$.products[0].price").value(39.90))
                    .andExpect(jsonPath("$.products[0].currency").value("BRL"))
                    .andExpect(jsonPath("$.paymentMethods.length()").value(3))
                    .andExpect(jsonPath("$.paymentMethods[0].name").value("Cartão de Crédito"))
                    .andExpect(jsonPath("$.paymentMethods[1].name").value("Cartão de Débito"))
                    .andExpect(jsonPath("$.paymentMethods[2].name").value("Pix"))
                    // a tela só recebe o nome do meio de pagamento
                    .andExpect(jsonPath("$.paymentMethods[0].type").doesNotExist());
        }

        @Test
        void produtoInexistenteDevolveFrontendError() throws Exception {
            assertErrorContract(postJson("/pre-checkout", "{\"products\":[{\"slug\":\"premium\"},{\"slug\":\"ultra\"}]}"),
                    422, "BLOCK", "PRODUCT_NOT_FOUND", "P-2001");
        }

        @Test
        void listaVaziaDevolveFrontendError() throws Exception {
            assertErrorContract(postJson("/pre-checkout", "{\"products\":[]}"), 400, "BLOCK", "INVALID_REQUEST", "P-0001");
        }

        @Test
        void servicoDeProdutosForaDoArDevolveFrontendError() throws Exception {
            when(productClient.findAll()).thenThrow(new ExternalServiceException("produtos", new RuntimeException("timeout")));
            assertErrorContract(postJson("/pre-checkout", "{\"products\":[{\"slug\":\"premium\"}]}"),
                    503, "BLOCK", "SERVICE_UNAVAILABLE", "P-9001");
        }
    }

    // ---------- checkout: sucesso ----------

    @Nested
    class CheckoutSucesso {

        @Test
        void cartaoDeCreditoAprovado() throws Exception {
            String cartId = createCart("premium");
            postJson("/checkout", cardPayment(cartId, TestData.CREDIT, "4111111111114242"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cartId").value(cartId))
                    .andExpect(jsonPath("$.status").value("APROVADO"))
                    .andExpect(jsonPath("$.paidAt").isNotEmpty())
                    .andExpect(jsonPath("$.payment.method").value("Cartão de Crédito"))
                    .andExpect(jsonPath("$.payment.lastDigits").value("4242"))
                    .andExpect(jsonPath("$.payment.pixCode").doesNotExist())
                    .andExpect(jsonPath("$.payment.amount").value(39.90))
                    .andExpect(jsonPath("$.payment.currency").value("BRL"))
                    .andExpect(jsonPath("$.products[0].slug").value("premium"))
                    .andExpect(jsonPath("$.products[0].name").value("Plano Premium"))
                    .andExpect(jsonPath("$.products[0].price").value(39.90));
        }

        @Test
        void cartaoDeDebitoAprovado() throws Exception {
            String cartId = createCart("basico");
            postJson("/checkout", cardPayment(cartId, TestData.DEBIT, "5555 6666 7777 1234"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payment.method").value("Cartão de Débito"))
                    .andExpect(jsonPath("$.payment.lastDigits").value("1234"))
                    .andExpect(jsonPath("$.payment.amount").value(19.90));
        }

        @Test
        void pixAprovadoTrazCodigoPixNoLugarDosUltimosDigitos() throws Exception {
            String cartId = createCart("familia", "basico");
            postJson("/checkout", pixPayment(cartId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APROVADO"))
                    .andExpect(jsonPath("$.payment.method").value("Pix"))
                    .andExpect(jsonPath("$.payment.lastDigits").doesNotExist())
                    .andExpect(jsonPath("$.payment.pixCode").isNotEmpty())
                    .andExpect(jsonPath("$.payment.amount").value(79.80))
                    .andExpect(jsonPath("$.products.length()").value(2));
        }

        @Test
        void naoGravaNumeroCompletoNemCvv() throws Exception {
            String cartId = createCart("premium");
            postJson("/checkout", cardPayment(cartId, TestData.CREDIT, "4111111111119876")).andExpect(status().isOk());

            List<Map<String, Object>> rows = jdbc.queryForList("select * from payments where cart_id = ?", UUID.fromString(cartId));
            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).get("last_digits")).isEqualTo("9876");
            assertThat(rows.get(0).toString()).doesNotContain("4111111111119876").doesNotContain("123,");
            assertThat(jdbc.queryForList(
                    "select column_name from information_schema.columns where lower(table_name) = 'payments'", String.class))
                    .noneMatch(c -> c.toLowerCase().contains("cvv") || c.toLowerCase().contains("number"));
        }
    }

    // ---------- checkout: erros ----------

    @Nested
    class CheckoutErros {

        @Test
        void cartaoTerminadoEm0002ERecusado() throws Exception {
            String cartId = createCart("premium");
            ResultActions result = postJson("/checkout", cardPayment(cartId, TestData.CREDIT, "4000000000000002"));
            assertErrorContract(result, 422, "BLOCK", "PAYMENT_DECLINED", "P-1001");
            result.andExpect(jsonPath("$.cartId").value(cartId))
                    .andExpect(jsonPath("$.frontendError.title").value("Pagamento não aprovado"))
                    .andExpect(jsonPath("$.frontendError.primaryButton.url").value("RETRY"))
                    .andExpect(jsonPath("$.frontendError.secondaryButton.url").value("/checkout/pagamento"));

            // a recusa fica registrada
            assertThat(jdbc.queryForObject("select status from payments where cart_id = ?", String.class,
                    UUID.fromString(cartId))).isEqualTo("DECLINED");
        }

        @Test
        void depoisDaRecusaPodeTentarDeNovoComOutroMeio() throws Exception {
            String cartId = createCart("premium");
            postJson("/checkout", cardPayment(cartId, TestData.CREDIT, "4000000000000002")).andExpect(status().is(422));
            postJson("/checkout", pixPayment(cartId)).andExpect(status().isOk());
        }

        @Test
        void carrinhoJaAprovadoNaoPodeSerPagoDeNovo() throws Exception {
            String cartId = createCart("premium");
            postJson("/checkout", cardPayment(cartId, TestData.CREDIT, "4111111111114242")).andExpect(status().isOk());

            assertErrorContract(postJson("/checkout", pixPayment(cartId)), 409, "BLOCK", "CART_ALREADY_PAID", "P-1004");
            assertThat(jdbc.queryForObject("select count(*) from payments where cart_id = ? and status = 'APPROVED'",
                    Integer.class, UUID.fromString(cartId))).isEqualTo(1);
        }

        @Test
        void carrinhoInexistente() throws Exception {
            ResultActions result = postJson("/checkout", pixPayment(UUID.randomUUID().toString()));
            assertErrorContract(result, 404, "BLOCK", "CART_NOT_FOUND", "P-3001");
        }

        @Test
        void cartIdMalformadoTambemECarrinhoInexistente() throws Exception {
            ResultActions result = postJson("/checkout", pixPayment("nao-e-uuid"));
            assertErrorContract(result, 404, "BLOCK", "CART_NOT_FOUND", "P-3001");
            result.andExpect(jsonPath("$.cartId").value("nao-e-uuid"));
        }

        @Test
        void meioDePagamentoQueNaoFoiOferecidoAoCarrinho() throws Exception {
            String cartId = createCart("premium");
            assertErrorContract(postJson("/checkout", cardPayment(cartId, "Boleto", "4111111111114242")),
                    422, "INTERVENTION", "PAYMENT_METHOD_NOT_ALLOWED", "P-1002");
        }

        @Test
        void meioRetiradoDoServicoDepoisDoPreCheckoutContinuaValendoParaOCarrinho() throws Exception {
            String cartId = createCart("premium");
            when(paymentMethodClient.findAll()).thenReturn(List.of(TestData.PAYMENT_METHODS.get(0)));
            postJson("/checkout", pixPayment(cartId)).andExpect(status().isOk());
        }

        @Test
        void pixComDadosDeCartaoERecusado() throws Exception {
            String cartId = createCart("premium");
            assertErrorContract(postJson("/checkout", cardPayment(cartId, TestData.PIX, "4111111111114242")),
                    400, "INTERVENTION", "INVALID_PAYMENT_DATA", "P-1003");
        }

        @Test
        void cartaoSemDados() throws Exception {
            String cartId = createCart("premium");
            String body = "{\"cartId\":\"" + cartId + "\",\"paymentMethod\":{\"name\":\"Cartão de Crédito\"}}";
            assertErrorContract(postJson("/checkout", body), 400, "INTERVENTION", "INVALID_PAYMENT_DATA", "P-1003");
        }

        @Test
        void cartaoVencido() throws Exception {
            String cartId = createCart("premium");
            String body = cardPayment(cartId, TestData.CREDIT, "4111111111114242").replace("12/40", "01/20");
            assertErrorContract(postJson("/checkout", body), 400, "INTERVENTION", "INVALID_PAYMENT_DATA", "P-1003");
        }

        @Test
        void corpoInvalidoUsaOMesmoObjetoDeErro() throws Exception {
            ResultActions result = postJson("/checkout", "{isso nao e json");
            assertErrorContract(result, 400, "BLOCK", "INVALID_REQUEST", "P-0001");
            result.andExpect(jsonPath("$.cartId").value(nullValue()));
        }
    }
}
