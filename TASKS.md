# Tasks do FinanSmart

## Autenticação: decisão do projeto

O login é feito pelo **Keycloak**. O FinanSmart não tem tabela de usuários e não guarda senhas nem emite tokens.

- **Identidade:** o dono de cada registro é o claim `sub` do token, gravado em `user_id`.
- **API:** atua como OAuth2 Resource Server e valida assinatura, emissor e validade do JWT emitido pelo Keycloak.
- **Angular:** usa Authorization Code com PKCE em um cliente público. Login e cadastro são telas do Keycloak, abertas por redirecionamento.
- **Tokens no navegador:** ficam em memória, nunca em `localStorage`. A renovação usa a sessão do Keycloak, mantida em cookie HttpOnly no domínio dele.

## Estado atual

- **T-00:** concluída, com monorepo, Compose, Flyway sem migrations, perfis dev/demo, ProblemDetail, CORS, Swagger, Angular e CI.
- **T-01:** a parte da API já existe (realm, cliente, validação de JWT e `CurrentUser`). Falta a integração no Angular.

## Separação das tasks

### Fundação

| Task | Entrega | Depende de |
|---|---|---|
| T-00 Setup do projeto | Monorepo `server/` + `web/`, Docker Compose com Postgres e Keycloak, Flyway, perfis dev/demo, handler global de erros (ProblemDetail), CORS, Swagger básico, GitHub Actions rodando os testes | — |
| T-01 Autenticação com Keycloak | Realm e cliente importados no Compose, API validando o JWT do Keycloak, usuário atual pelo `sub`; no Angular, login, cadastro e logout por redirecionamento com PKCE, guard e interceptor | T-00 |

### MVP

| Task | Entrega | Depende de |
|---|---|---|
| T-02 Contas | CRUD + arquivar; tela de contas | T-01 |
| T-03 Categorias | Categorias padrão do sistema + personalizadas por usuário; tela de categorias | T-01 |
| T-04 Lançamentos | Receitas e despesas com conta, categoria, data e descrição; filtros por período/conta/categoria; tela com lista paginada | T-02, T-03 |
| T-05 Saldo e painel | Saldo atual por conta (saldo inicial + lançamentos) e total; painel inicial | T-04 |
| T-06 Orçamentos | Limite mensal por categoria, com % consumido e alerta ao passar de 80%/100% | T-04 |
| T-07 Relatório mensal | Receitas × despesas, gastos por categoria, comparação com o mês anterior; gráficos | T-04 |
| T-08 Detector de vazamentos | Assinaturas recorrentes, tarifas e juros, gasto acima da média; card "R$ X vazando este mês" | T-04 |
| T-09 Dados de demonstração | Perfil demo com um usuário no Keycloak (id fixo no realm importado) e 6 meses de lançamentos realistas ligados a esse id, para a demo pública mostrar os detectores funcionando | T-08 |

### Acabamento

| Task | Entrega | Depende de |
|---|---|---|
| T-10 Documentação da API | Swagger com descrições, exemplos, erros documentados e autenticação pelo Keycloak | MVP |
| T-11 README e ADRs | README com arquitetura, como rodar, prints/GIF, link da demo; ADRs das decisões principais, incluindo o uso do Keycloak | MVP |
| T-12 Deploy da demo | Front, back e Keycloak publicados em modo demo, com HTTPS e Keycloak em modo de produção (`start`); link no README | T-09 |

### v1.1 (depois do MVP publicado)

Metas · transferências entre contas · importação CSV · integração Pluggy · explicações com Spring AI · cartão de crédito

---

## T-02 · Contas

### Objetivo

Permitir que o usuário cadastre e gerencie as contas onde o dinheiro dele está. Lançamentos (T-04) e saldo (T-05) dependem desta task.

### Depende de

T-01 Autenticação com Keycloak: o usuário logado, identificado pelo `sub` do token, é o dono da conta.

### Fora de escopo

Cálculo do saldo atual (T-05) · cartão de crédito · transferências · Pluggy · outras moedas.

### Regras de negócio

| # | Regra |
|---|---|
| RN-01 | Nome obrigatório, de 1 a 60 caracteres, sem espaços nas pontas |
| RN-02 | Nome único por usuário entre as contas ativas, sem diferenciar maiúsculas de minúsculas |
| RN-03 | Tipos: `CHECKING` (corrente), `SAVINGS` (poupança), `WALLET` (carteira), `INVESTMENT` (investimento) |
| RN-04 | Saldo inicial obrigatório; pode ser zero ou negativo; no máximo 2 casas decimais |
| RN-05 | Moeda fixa em BRL |
| RN-06 | Conta arquivada sai da listagem padrão, mas os dados continuam existindo |
| RN-07 | Só pode ser excluída se não tiver lançamentos. Com lançamentos, só arquivar (passa a valer de fato na T-04) |
| RN-08 | Conta de outro usuário responde 404, não 403, para não revelar que o id existe |

### Backend

Migration `V1__create_accounts.sql` (não há migration de usuários, porque eles ficam no Keycloak):

```sql
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
```

- `user_id` guarda o `sub` do Keycloak. É texto porque o padrão OIDC define o `sub` como string, e não tem chave estrangeira porque o usuário está fora deste banco.
- `NUMERIC(19,2)` + `BigDecimal`: dinheiro nunca é `double`.
- Índice único parcial: a RN-02 é garantida pelo banco, não só pelo código.
- `version` com `@Version`: lock otimista, para uma edição não sobrescrever outra em silêncio.

Endpoints em `/api/v1/accounts`, todos autenticados com o token do Keycloak:

| Método | Rota | Sucesso | Erros |
|---|---|---|---|
| POST | `/` | 201 + Location | 400, 409 |
| GET | `/?includeArchived=false` | 200 | — |
| GET | `/{id}` | 200 | 404 |
| PUT | `/{id}` | 200 | 400, 404, 409 |
| PATCH | `/{id}/archive` | 204 | 404 |
| PATCH | `/{id}/unarchive` | 204 | 404, 409 |
| DELETE | `/{id}` | 204 | 404, 409 |

Request:

```json
{ "name": "Nubank", "type": "CHECKING", "initialBalance": 1500.00 }
```

Response:

```json
{
  "id": "6f1c...",
  "name": "Nubank",
  "type": "CHECKING",
  "initialBalance": 1500.00,
  "currency": "BRL",
  "archived": false,
  "createdAt": "2026-11-09T16:30:00Z",
  "updatedAt": "2026-11-09T16:30:00Z"
}
```

Erros usam o ProblemDetail configurado na T-00.

Estrutura:

```text
account/
├── Account.java
├── AccountType.java
├── AccountRepository.java
├── AccountService.java
├── AccountController.java
├── dto/  CreateAccountRequest, UpdateAccountRequest, AccountResponse  (records)
└── exception/  AccountNotFoundException, DuplicateAccountNameException
```

### Frontend (Angular)

```text
web/src/app/features/accounts/
├── accounts.routes.ts          ← rota lazy, protegida pelo guard do Keycloak
├── account.service.ts          ← chamadas HTTP, com token anexado pelo interceptor
├── account.model.ts
├── account-list/               ← lista + ações
└── account-form/               ← criar/editar (dialog ou página)
```

- Lista com nome, tipo e saldo inicial formatado (R$ 1.500,00), e um filtro "mostrar arquivadas"
- Formulário com Reactive Forms e as mesmas validações do backend; o erro 409 aparece no campo nome
- Confirmação antes de arquivar e de excluir
- Estados de carregando, vazio ("Nenhuma conta ainda. Crie a primeira.") e erro com "tentar de novo"
- Estado da tela com signals; templates com `@if`/`@for`

### Testes

**Backend, unitários:** cria conta válida · rejeita nome duplicado entre ativas · permite reutilizar nome de conta arquivada · desarquivar com nome em conflito falha.

**Backend, integração (Testcontainers + MockMvc):** fluxo completo criar → listar → editar → arquivar → desarquivar → excluir · validações retornam 400 com os campos · usuário A recebe 404 nas contas do usuário B, simulando tokens com `sub` diferentes por `jwt()` do Spring Security Test · sem token, 401.

**Frontend:** testes do `AccountService` e do formulário (validações e exibição do erro 409).

### Critérios de aceite

- [ ] Migration aplicada pelo Flyway na subida
- [ ] Os 7 endpoints conforme a tabela
- [ ] RN-01 a RN-08 cobertas por testes
- [ ] Teste de isolamento entre usuários passando
- [ ] Tela de contas funcionando contra a API real, com login real no Keycloak e os quatro estados
- [ ] Endpoints visíveis no Swagger
- [ ] CI verde

### Git

Branch `feat/T-02-contas` · commits sugeridos:

- `feat(accounts): migration`
- `feat(accounts): domínio e serviço`
- `feat(accounts): endpoints REST`
- `test(accounts): testes`
- `feat(web): tela de contas`
