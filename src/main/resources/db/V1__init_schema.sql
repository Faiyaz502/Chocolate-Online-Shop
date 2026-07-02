-- =========================================================
-- Chocolate Shop - Core Schema (Phase 1 foundation)
-- Only `users` and `refresh_tokens` are used by Java code
-- in Phase 1. The remaining tables are created now so the
-- schema is stable and later phases only ADD columns/tables
-- instead of reshaping existing ones.
-- =========================================================

-- ---------- USERS & AUTH ----------

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    first_name      VARCHAR(100)  NOT NULL,
    last_name       VARCHAR(100)  NOT NULL,
    email           VARCHAR(255)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255)  NOT NULL,
    phone           VARCHAR(30),
    role            VARCHAR(20)   NOT NULL DEFAULT 'CUSTOMER' CHECK (role IN ('CUSTOMER', 'ADMIN')),
    enabled         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users(email);

CREATE TABLE refresh_tokens (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token           VARCHAR(512)  NOT NULL UNIQUE,
    expires_at      TIMESTAMP     NOT NULL,
    revoked         BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);

CREATE TABLE addresses (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label           VARCHAR(50),
    recipient_name  VARCHAR(150)  NOT NULL,
    phone           VARCHAR(30)   NOT NULL,
    line1           VARCHAR(255)  NOT NULL,
    line2           VARCHAR(255),
    city            VARCHAR(100)  NOT NULL,
    state           VARCHAR(100),
    postal_code     VARCHAR(20)   NOT NULL,
    country         VARCHAR(100)  NOT NULL,
    is_default      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_addresses_user_id ON addresses(user_id);

-- ---------- CATALOG ----------

CREATE TABLE categories (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100)  NOT NULL UNIQUE,
    slug            VARCHAR(120)  NOT NULL UNIQUE,
    description     VARCHAR(500),
    created_at      TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE TABLE products (
    id              BIGSERIAL PRIMARY KEY,
    category_id     BIGINT        NOT NULL REFERENCES categories(id),
    name            VARCHAR(200)  NOT NULL,
    slug            VARCHAR(220)  NOT NULL UNIQUE,
    description     TEXT,
    price           NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    discount_price  NUMERIC(10,2) CHECK (discount_price >= 0),
    stock_quantity  INTEGER       NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_is_active ON products(is_active);

CREATE TABLE product_images (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_url       VARCHAR(500)  NOT NULL,
    display_order   INTEGER       NOT NULL DEFAULT 0,
    is_primary      BOOLEAN       NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_product_images_product_id ON product_images(product_id);

-- ---------- CART ----------

CREATE TABLE carts (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE TABLE cart_items (
    id              BIGSERIAL PRIMARY KEY,
    cart_id         BIGINT        NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id      BIGINT        NOT NULL REFERENCES products(id),
    quantity        INTEGER       NOT NULL CHECK (quantity > 0),
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    UNIQUE (cart_id, product_id)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);

-- ---------- COUPONS ----------

CREATE TABLE coupons (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(50)   NOT NULL UNIQUE,
    discount_percent    NUMERIC(5,2)  CHECK (discount_percent > 0 AND discount_percent <= 100),
    discount_amount     NUMERIC(10,2) CHECK (discount_amount > 0),
    valid_from          TIMESTAMP     NOT NULL,
    valid_until         TIMESTAMP     NOT NULL,
    max_uses            INTEGER,
    times_used          INTEGER       NOT NULL DEFAULT 0,
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE
);

-- ---------- ORDERS ----------

CREATE TABLE orders (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT        NOT NULL REFERENCES users(id),
    order_number        VARCHAR(40)   NOT NULL UNIQUE,
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING','PROCESSING','SHIPPED','DELIVERED','CANCELLED')),
    subtotal            NUMERIC(10,2) NOT NULL,
    discount_total      NUMERIC(10,2) NOT NULL DEFAULT 0,
    shipping_fee        NUMERIC(10,2) NOT NULL DEFAULT 0,
    grand_total         NUMERIC(10,2) NOT NULL,
    coupon_id           BIGINT        REFERENCES coupons(id),
    shipping_address_id BIGINT        NOT NULL REFERENCES addresses(id),
    created_at          TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);

CREATE TABLE order_items (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id      BIGINT        NOT NULL REFERENCES products(id),
    product_name    VARCHAR(200)  NOT NULL,   -- snapshot at time of order
    unit_price      NUMERIC(10,2) NOT NULL,
    quantity        INTEGER       NOT NULL CHECK (quantity > 0),
    line_total      NUMERIC(10,2) NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);

CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT        NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    method          VARCHAR(20)   NOT NULL CHECK (method IN ('CARD','COD')),
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING','PAID','FAILED','REFUNDED')),
    provider_reference VARCHAR(255),
    amount          NUMERIC(10,2) NOT NULL,
    paid_at         TIMESTAMP,
    created_at      TIMESTAMP     NOT NULL DEFAULT now()
);
