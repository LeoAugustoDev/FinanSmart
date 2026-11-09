CREATE TABLE transactions (
    id          UUID           PRIMARY KEY,
    user_id     VARCHAR(255)   NOT NULL,
    account_id  UUID           NOT NULL REFERENCES accounts (id),
    category_id UUID           NOT NULL REFERENCES categories (id),
    amount      NUMERIC(19, 2) NOT NULL,
    occurred_on DATE           NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ    NOT NULL,
    updated_at  TIMESTAMPTZ    NOT NULL,
    version     BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT ck_transactions_amount_positive CHECK (amount > 0)
);

CREATE INDEX ix_transactions_user_date ON transactions (user_id, occurred_on DESC);
CREATE INDEX ix_transactions_account ON transactions (account_id);
CREATE INDEX ix_transactions_category ON transactions (category_id);
