CREATE TABLE accounts (
    id              UUID           PRIMARY KEY,
    user_id         VARCHAR(255)   NOT NULL,
    name            VARCHAR(60)    NOT NULL,
    type            VARCHAR(20)    NOT NULL,
    initial_balance NUMERIC(19, 2) NOT NULL,
    currency        CHAR(3)        NOT NULL DEFAULT 'BRL',
    archived        BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL,
    version         BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT ck_accounts_type
        CHECK (type IN ('CHECKING', 'SAVINGS', 'WALLET', 'INVESTMENT'))
);

CREATE INDEX ix_accounts_user ON accounts (user_id);

CREATE UNIQUE INDEX ux_accounts_user_name_active
    ON accounts (user_id, LOWER(name))
    WHERE archived = FALSE;
