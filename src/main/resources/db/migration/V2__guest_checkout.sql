-- =========================================================
-- Guest checkout support: carts, orders, and addresses can now
-- belong to either a registered user OR an anonymous guest,
-- identified by IP address instead of a user_id.
-- =========================================================

-- ---------- CARTS ----------
ALTER TABLE carts ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE carts ADD COLUMN ip_address VARCHAR(45);
ALTER TABLE carts ADD CONSTRAINT uq_carts_ip_address UNIQUE (ip_address);
ALTER TABLE carts ADD CONSTRAINT chk_carts_owner CHECK (
    (user_id IS NOT NULL AND ip_address IS NULL) OR
    (user_id IS NULL AND ip_address IS NOT NULL)
);

-- ---------- ADDRESSES ----------
-- guest checkout addresses aren't saved to any account; they're only
-- referenced by the order that used them
ALTER TABLE addresses ALTER COLUMN user_id DROP NOT NULL;

-- ---------- ORDERS ----------
ALTER TABLE orders ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE orders ADD COLUMN ip_address VARCHAR(45);
ALTER TABLE orders ADD COLUMN guest_email VARCHAR(255);
ALTER TABLE orders ADD CONSTRAINT chk_orders_owner CHECK (
    (user_id IS NOT NULL AND ip_address IS NULL) OR
    (user_id IS NULL AND ip_address IS NOT NULL)
);

CREATE INDEX idx_orders_ip_address ON orders(ip_address);
CREATE INDEX idx_carts_ip_address ON carts(ip_address);
