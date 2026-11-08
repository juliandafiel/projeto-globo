-- Carrinho criado no pré-checkout. Guarda uma cópia dos produtos e dos meios de pagamento
-- oferecidos naquele momento: o checkout valida contra essa cópia, não contra o catálogo atual.
CREATE TABLE carts (
    id           UUID          PRIMARY KEY,
    status       VARCHAR(20)   NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    currency     VARCHAR(3)    NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    paid_at      TIMESTAMP WITH TIME ZONE,
    version      BIGINT        NOT NULL DEFAULT 0
);

CREATE TABLE cart_items (
    id          BIGSERIAL     PRIMARY KEY,
    cart_id     UUID          NOT NULL REFERENCES carts (id),
    position    INTEGER       NOT NULL,
    slug        VARCHAR(60)   NOT NULL,
    name        VARCHAR(120)  NOT NULL,
    description VARCHAR(255),
    price       NUMERIC(12,2) NOT NULL,
    currency    VARCHAR(3)    NOT NULL
);
CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);

CREATE TABLE cart_payment_methods (
    cart_id  UUID        NOT NULL REFERENCES carts (id),
    position INTEGER     NOT NULL,
    code     VARCHAR(40) NOT NULL,
    name     VARCHAR(60) NOT NULL,
    type     VARCHAR(20) NOT NULL,
    PRIMARY KEY (cart_id, position)
);

-- Cada tentativa de pagamento (aprovada ou recusada). Do cartão, só os 4 últimos dígitos:
-- número completo e CVV nunca são gravados.
CREATE TABLE payments (
    id             UUID          PRIMARY KEY,
    cart_id        UUID          NOT NULL REFERENCES carts (id),
    method_name    VARCHAR(60)   NOT NULL,
    method_type    VARCHAR(20)   NOT NULL,
    status         VARCHAR(20)   NOT NULL,
    amount         NUMERIC(12,2) NOT NULL,
    currency       VARCHAR(3)    NOT NULL,
    last_digits    VARCHAR(4),
    pix_code       VARCHAR(255),
    decline_reason VARCHAR(60),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_payments_cart ON payments (cart_id);
