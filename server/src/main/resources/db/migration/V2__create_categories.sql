CREATE TABLE categories (
    id         UUID         PRIMARY KEY,
    user_id    VARCHAR(255),
    name       VARCHAR(60)  NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_categories_type CHECK (type IN ('INCOME', 'EXPENSE'))
);

CREATE INDEX ix_categories_user ON categories (user_id);

-- user_id nulo identifica uma categoria padrão, visível para todos os usuários.
CREATE UNIQUE INDEX ux_categories_user_name_type
    ON categories (user_id, LOWER(name), type)
    WHERE user_id IS NOT NULL;

CREATE UNIQUE INDEX ux_categories_system_name_type
    ON categories (LOWER(name), type)
    WHERE user_id IS NULL;

INSERT INTO categories (id, user_id, name, type, created_at, updated_at)
SELECT gen_random_uuid(), NULL, seed.name, seed.type, NOW(), NOW()
FROM (VALUES
    ('Alimentação', 'EXPENSE'),
    ('Moradia', 'EXPENSE'),
    ('Transporte', 'EXPENSE'),
    ('Saúde', 'EXPENSE'),
    ('Educação', 'EXPENSE'),
    ('Lazer', 'EXPENSE'),
    ('Compras', 'EXPENSE'),
    ('Assinaturas', 'EXPENSE'),
    ('Tarifas e juros', 'EXPENSE'),
    ('Outras despesas', 'EXPENSE'),
    ('Salário', 'INCOME'),
    ('Freelance', 'INCOME'),
    ('Rendimentos', 'INCOME'),
    ('Outras receitas', 'INCOME')
) AS seed (name, type);
