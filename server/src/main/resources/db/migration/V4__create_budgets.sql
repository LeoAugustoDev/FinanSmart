CREATE TABLE budgets (
    id              UUID           PRIMARY KEY,
    user_id         VARCHAR(255)   NOT NULL,
    category_id     UUID           NOT NULL REFERENCES categories (id),
    reference_month DATE           NOT NULL,
    limit_amount    NUMERIC(19, 2) NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL,
    version         BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT ck_budgets_first_day_of_month CHECK (EXTRACT(DAY FROM reference_month) = 1),
    CONSTRAINT ck_budgets_limit_positive CHECK (limit_amount > 0),
    CONSTRAINT ux_budgets_user_category_month UNIQUE (user_id, category_id, reference_month)
);

CREATE INDEX ix_budgets_category ON budgets (category_id);
