# Checkout BFF

BFF (Backend for Frontend) de um checkout de assinatura, com duas etapas:

1. **Pré-checkout** (`POST /operations/pre-checkout`): monta o carrinho a partir dos planos escolhidos, consultando o **serviço de produtos** e o **serviço de meios de pagamento**.
2. **Checkout** (`POST /operations/checkout`): conclui o pagamento (cobrança simulada) e devolve os dados da **tela de sucesso**, ou um erro no formato único `frontendError`.

Stack: Java 17+ · Spring Boot 4 · PostgreSQL + Flyway · Redis (cache) · RabbitMQ (eventos) · JUnit 5 + Mockito · Docker.

---

## Como subir

### Opção 1: Docker (recomendado)

Requisito: Docker com Compose.

```bash
docker compose up -d --build
curl localhost:8080/actuator/health        # {"status":"UP"}
```

Sobem a API (`:8080`), o PostgreSQL (`:5433`), o Redis (`:6380`) e o RabbitMQ (`:5672`, painel em http://localhost:15672, usuário e senha `guest`).

Para parar: `docker compose down` (com `-v`, apaga também os dados).

### Opção 2: Rodar a API na máquina

Requisito: JDK 17 ou superior. O Maven não precisa estar instalado (o projeto usa o wrapper `./mvnw`).

```bash
# só a infraestrutura pelo Docker
docker compose up -d postgres redis

DB_PORT=5433 DB_NAME=checkout DB_USER=checkout DB_PASSWORD=checkout REDIS_PORT=6380 REDIS_DATABASE=0 \
  ./mvnw spring-boot:run
```

Sem `EVENTS_BROKER=rabbit`, os eventos só vão para o log e o RabbitMQ não é necessário.

### Testes

```bash
./mvnw test
```

Os testes não precisam de Docker: usam H2 em memória (modo PostgreSQL, com as mesmas migrations do Flyway) e cache em memória.

---

## Como simular sucesso e recusa

| Cenário | Como provocar |
|---|---|
| Cartão aprovado | qualquer cartão válido que **não** termine em `0002` (ex.: `4111111111114242`) |
| Cartão recusado | número terminado em **`0002`** (ex.: `4000000000000002`) |
| Pix aprovado | `"paymentMethod": {"name": "Pix"}`, sem `card` |
| Pagamento duplicado | pagar de novo um carrinho já aprovado |
| Carrinho inexistente | `cartId` que não veio do pré-checkout |
| Produto inexistente | slug fora de `basico`, `premium` e `familia` no pré-checkout |

O cartão precisa ter titular, número com 13 a 19 dígitos, validade `MM/AA` ou `MM/AAAA` não vencida e CVV de 3 ou 4 dígitos. Fora disso, a resposta é `INVALID_PAYMENT_DATA`.

O arquivo [`requests.http`](requests.http) tem todos os cenários prontos (IntelliJ, VS Code REST Client). Também dá para usar curl:

```bash
# 1) pré-checkout
curl -s -X POST localhost:8080/operations/pre-checkout \
  -H 'Content-Type: application/json' \
  -d '{"products":[{"slug":"premium"}]}'

# 2) checkout aprovado (troque o cartId pelo que voltou acima)
curl -s -X POST localhost:8080/operations/checkout \
  -H 'Content-Type: application/json' \
  -d '{"cartId":"<cartId>","paymentMethod":{"name":"Cartão de Crédito",
       "card":{"holderName":"Ana Souza","number":"4111111111114242","expiry":"12/28","cvv":"123"}}}'

# 3) checkout recusado: mesmo corpo, com número terminado em 0002
```

---

## Contrato

### `POST /operations/pre-checkout` → `201 Created`

```json
{ "products": [{ "slug": "premium" }] }
```

```json
{
  "cartId": "f1d4c35a-8634-43ed-80e6-4d13c4271dbc",
  "products": [
    { "slug": "premium", "name": "Plano Premium", "description": "Assinatura mensal com catálogo completo", "price": 39.90, "currency": "BRL" }
  ],
  "paymentMethods": [ { "name": "Cartão de Crédito" }, { "name": "Cartão de Débito" }, { "name": "Pix" } ]
}
```

Catálogo do serviço de produtos (mock): `basico` (R$ 19,90), `premium` (R$ 39,90) e `familia` (R$ 59,90).

### `POST /operations/checkout` → `200 OK`

Cartão:

```json
{
  "cartId": "f1d4c35a-8634-43ed-80e6-4d13c4271dbc",
  "status": "APROVADO",
  "paidAt": "2026-10-14T16:00:00Z",
  "payment": { "method": "Cartão de Crédito", "lastDigits": "4242", "amount": 39.90, "currency": "BRL" },
  "products": [ { "slug": "premium", "name": "Plano Premium", "price": 39.90 } ]
}
```

Pix: `lastDigits` é substituído por `pixCode`, um "copia e cola" simulado no formato EMV do BR Code, com CRC16:

```json
"payment": { "method": "Pix", "pixCode": "00020126580014br.gov.bcb.pix0136…6304B1C2", "amount": 39.90, "currency": "BRL" }
```

### Erros: sempre o mesmo objeto

```json
{
  "displayType": "BLOCK",
  "errorCode": "PAYMENT_DECLINED",
  "cartId": "f1d4c35a-8634-43ed-80e6-4d13c4271dbc",
  "frontendError": {
    "code": "P-1001",
    "title": "Pagamento não aprovado",
    "description": "Não foi possível concluir o pagamento. Tente novamente ou escolha outro meio.",
    "primaryButton":   { "label": "Tentar novamente", "url": "RETRY" },
    "secondaryButton": { "label": "Trocar meio de pagamento", "url": "/checkout/pagamento" }
  }
}
```

As chaves são sempre as mesmas. `cartId` e `secondaryButton` vêm `null` quando não se aplicam, e o formato não muda de um erro para outro. `RETRY` no `url` indica que o frontend deve repetir a última ação.

| errorCode | HTTP | displayType | code | Quando |
|---|---|---|---|---|
| `PAYMENT_DECLINED` | 422 | BLOCK | P-1001 | cobrança recusada (cartão final `0002`) |
| `PAYMENT_METHOD_NOT_ALLOWED` | 422 | INTERVENTION | P-1002 | meio não oferecido no pré-checkout daquele carrinho |
| `INVALID_PAYMENT_DATA` | 400 | INTERVENTION | P-1003 | cartão ausente ou inválido, ou dados de cartão enviados no Pix |
| `CART_ALREADY_PAID` | 409 | BLOCK | P-1004 | carrinho já aprovado (pagamento duplicado) |
| `PRODUCT_NOT_FOUND` | 422 | BLOCK | P-2001 | slug inexistente no pré-checkout |
| `CART_NOT_FOUND` | 404 | BLOCK | P-3001 | `cartId` inexistente ou malformado |
| `INVALID_REQUEST` | 400 | BLOCK | P-0001 | JSON inválido ou campos obrigatórios ausentes |
| `SERVICE_UNAVAILABLE` | 503 | BLOCK | P-9001 | serviço de produtos ou de meios de pagamento fora do ar |
| `UNEXPECTED_ERROR` | 500 | BLOCK | P-9999 | erro não previsto |

Todas as falhas de negócio saem com **4xx**. As duas últimas linhas não são culpa do pedido (dependência fora do ar, bug), por isso saem 5xx, mas no **mesmo objeto**. O frontend trata todas do mesmo jeito.

---

## Arquitetura

```
api/            Controller, DTOs de entrada e saída, catálogo de erros e handler único
application/    Regras: PreCheckoutService, CheckoutService, CardValidator
domain/         ErrorCode, exceções de negócio, PaymentMethodType
client/         Portas para os serviços externos (ProductClient, PaymentMethodClient) + implementações HTTP com cache
gateway/        Porta de cobrança (PaymentGateway) + SimulatedPaymentGateway
persistence/    Entidades JPA (Cart, CartItem, Payment) e repositórios
events/         Evento de pagamento aprovado → RabbitMQ (ou log) + consumidor
mock/           Serviços externos simulados, expostos por HTTP em /mock
config/         RestClient com timeouts, cache, Clock
```

```
Frontend ──► OperationsController ──► PreCheckoutService ──► ProductClient ───────► GET /mock/products
                                  │                      └─► PaymentMethodClient ─► GET /mock/payment-methods
                                  │                           (Redis na frente dos dois)
                                  └─► CheckoutService ──► PaymentGateway (simulado)
                                                      ├─► PostgreSQL (carrinho e tentativas)
                                                      └─► evento após commit ──► RabbitMQ ──► SubscriptionActivationConsumer
```

- **Serviços externos atrás de interfaces.** O BFF só conhece `ProductClient` e `PaymentMethodClient`. As implementações chamam por HTTP os mocks em `/mock/*`, que sobem junto com a aplicação. Para usar serviços reais, basta configurar `PRODUCTS_URL` e `PAYMENT_METHODS_URL` e desligar os mocks com `MOCK_SERVICES_ENABLED=false`. Nada nos endpoints muda.
- **O carrinho guarda uma cópia do que foi oferecido.** Preços e meios de pagamento são gravados no pré-checkout. O checkout valida o meio contra essa cópia ("um dos devolvidos no pré-checkout daquele carrinho") e cobra o valor que o usuário viu, mesmo que o catálogo mude depois.
- **Pagamento duplicado.** O checkout carrega o carrinho com `SELECT … FOR UPDATE`. Duas requisições simultâneas do mesmo carrinho ficam em fila, e a segunda já o encontra pago (`409 CART_ALREADY_PAID`). Há ainda `@Version` como segunda proteção.
- **Recusa não fecha o carrinho.** A tentativa recusada é gravada (`payments.status = DECLINED`) e o carrinho segue aberto para "Tentar novamente" ou "Trocar meio de pagamento".
- **Cartão.** O número completo e o CVV só existem em memória durante a cobrança. A tabela `payments` não tem coluna para eles, só `last_digits`, e os `toString()` dos objetos de cartão mascaram os dados para não vazarem em log. Há um teste que confere isso no banco.
- **Cache (Redis).** As consultas de produtos e meios de pagamento ficam em cache por 10 min (`CACHE_TTL`). Se o Redis cair, o BFF registra o aviso e consulta o serviço direto: o cache nunca derruba o checkout.
- **Eventos (RabbitMQ).** Quando o pagamento é aprovado, o evento `payment.approved` é publicado na exchange `checkout.events` **depois do commit**, para nunca anunciar pagamento não gravado. O consumidor (`SubscriptionActivationConsumer`) simula o processamento assíncrono pós-pagamento (ativar assinatura, recibo). Mensagem com erro vai para `checkout.subscription-activation.dlq`. A resposta ao frontend continua síncrona, porque a tela de sucesso precisa do resultado da cobrança na hora.
- **Timeouts.** Os clientes HTTP têm timeout de conexão (1 s) e de leitura (2 s). Falha vira `SERVICE_UNAVAILABLE` no formato `frontendError`.

## Variáveis de ambiente

| Variável | Padrão | Uso |
|---|---|---|
| `PORT` | `8080` | porta da API |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `localhost` / `5432` / `globo` / `globo` / `globo` | PostgreSQL |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_DATABASE` | `localhost` / `6379` / `5` | Redis |
| `CACHE_TYPE` | `redis` | `simple` usa cache em memória (sem Redis) |
| `CACHE_TTL` | `10m` | validade do cache de produtos e meios |
| `EVENTS_BROKER` | `log` | `rabbit` publica no RabbitMQ |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | `localhost` / `5672` / `guest` / `guest` | RabbitMQ |
| `PRODUCTS_URL` / `PAYMENT_METHODS_URL` | `http://localhost:${PORT}/mock` | serviços externos |
| `MOCK_SERVICES_ENABLED` | `true` | sobe os mocks em `/mock` |

## O que ficou de fora

- Gateway real de pagamento: a cobrança é simulada, como pede o desafio.
- Validação de Luhn no número do cartão: o exemplo do desafio (`4111111111114242`) não passa no Luhn, então só o formato é validado.
- Expiração de carrinho, autenticação e idempotency key por requisição. Seriam os próximos passos num cenário real.
