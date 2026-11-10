# Tasks do FinanSmart

## Autenticação: decisão do projeto

O login é feito pelo **Keycloak**. O FinanSmart não tem tabela de usuários e não guarda senhas nem emite tokens.

- **Identidade:** o dono de cada registro é o claim `sub` do token, gravado em `user_id`.
- **API:** atua como OAuth2 Resource Server e valida assinatura, emissor e validade do JWT emitido pelo Keycloak.
- **Angular:** usa Authorization Code com PKCE em um cliente público. Login e cadastro são telas do Keycloak, abertas por redirecionamento.
- **Tokens no navegador:** ficam em memória, nunca em `localStorage`. A renovação usa a sessão do Keycloak, mantida em cookie HttpOnly no domínio dele.

## Estado atual

- **T-00:** concluída, com monorepo, Compose, Flyway sem migrations, perfis dev/demo, ProblemDetail, CORS, Swagger e Angular.
- **T-01:** concluída, com login, cadastro e logout no Keycloak, guard, interceptor e endpoint `/api/v1/me`.
- **T-02:** concluída.
- **T-03:** concluída.
- **T-04:** concluída, incluindo o bloqueio de exclusão de contas e categorias com lançamentos.
- **T-05:** concluída.
- **T-06:** concluída.
- **T-07:** concluída.
- **T-08:** concluída.
- **T-09:** concluída, com carga única de seis meses no perfil demo.

## Separação das tasks

### Fundação

| Task | Entrega | Depende de |
|---|---|---|
| T-00 Setup do projeto | Monorepo `server/` + `web/`, Docker Compose com Postgres e Keycloak, Flyway, perfis dev/demo, handler global de erros (ProblemDetail), CORS, Swagger básico | — |
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

- [x] Migration aplicada pelo Flyway na subida
- [x] Os 7 endpoints conforme a tabela
- [x] RN-01 a RN-08 cobertas por testes
- [x] Teste de isolamento entre usuários passando
- [x] Tela de contas funcionando contra a API real, com login real no Keycloak e os quatro estados
- [x] Endpoints visíveis no Swagger
- [x] Testes do backend e do frontend passando

### Git

Branch `feat/T-02-contas` · commits sugeridos:

- `feat(accounts): migration`
- `feat(accounts): domínio e serviço`
- `feat(accounts): endpoints REST`
- `test(accounts): testes`
- `feat(web): tela de contas`

---

## T-03 · Categorias

### Objetivo

Classificar receitas e despesas com categorias padrão do sistema e categorias criadas por cada usuário. Lançamentos (T-04), orçamentos (T-06) e relatórios (T-07) dependem desta task.

### Depende de

T-01 Autenticação com Keycloak.

### Regras de negócio

| # | Regra |
|---|---|
| RN-01 | Nome obrigatório, de 1 a 60 caracteres, sem espaços nas pontas |
| RN-02 | Tipos: `EXPENSE` (despesa) e `INCOME` (receita) |
| RN-03 | Nome único por tipo entre as categorias que o usuário vê (padrão e próprias), sem diferenciar maiúsculas de minúsculas |
| RN-04 | Categorias padrão são visíveis para todos e somente leitura: alterar ou excluir responde 403 |
| RN-05 | O tipo não muda depois de criado, porque define o sinal dos lançamentos já feitos |
| RN-06 | Categoria pessoal de outro usuário responde 404 |
| RN-07 | Só pode ser excluída se não tiver lançamentos (passa a valer de fato na T-04) |

### Backend

Migration `V2__create_categories.sql`, com `user_id` nulo nas categorias padrão e dois índices únicos parciais.

Endpoints em `/api/v1/categories`, todos autenticados:

| Método | Rota | Sucesso | Erros |
|---|---|---|---|
| POST | `/` | 201 + Location | 400, 409 |
| GET | `/?type=` | 200 | 400 |
| GET | `/{id}` | 200 | 404 |
| PUT | `/{id}` (só o nome) | 200 | 400, 403, 404, 409 |
| DELETE | `/{id}` | 204 | 403, 404, 409 |

### Frontend (Angular)

Tela em `/categorias` com filtro por tipo, selo "Padrão" e ações apenas nas categorias pessoais. Formulário com o tipo bloqueado na edição.

### Critérios de aceite

- [x] Migration aplicada pelo Flyway na subida, com as categorias padrão
- [x] Os 5 endpoints conforme a tabela
- [x] RN-01 a RN-07 cobertas por testes
- [x] Teste de isolamento entre usuários passando
- [x] Tela de categorias funcionando contra a API real
- [x] Endpoints visíveis no Swagger
- [x] Testes do backend e do frontend passando

---

## T-04 · Lançamentos

### Objetivo

Registrar receitas e despesas em uma conta, com categoria, data e descrição. Saldo (T-05), orçamentos (T-06), relatório (T-07) e detector de vazamentos (T-08) dependem desta task.

### Depende de

T-02 Contas e T-03 Categorias.

### Regras de negócio

| # | Regra |
|---|---|
| RN-01 | Conta, categoria, valor e data são obrigatórios; descrição é opcional, com até 255 caracteres |
| RN-02 | Valor maior que zero, com no máximo 2 casas decimais |
| RN-03 | Receita ou despesa vem do tipo da categoria, sem campo próprio |
| RN-04 | A conta deve ser do usuário, e a categoria deve ser padrão ou do usuário; do contrário responde 400 no campo |
| RN-05 | Conta arquivada não recebe novos lançamentos, mas os que já estão nela podem ser editados |
| RN-06 | Lançamento de outro usuário responde 404 |
| RN-07 | Conta ou categoria com lançamentos não pode ser excluída: responde 409 |

### Backend

Migration `V3__create_transactions.sql`.

Endpoints em `/api/v1/transactions`, todos autenticados:

| Método | Rota | Sucesso | Erros |
|---|---|---|---|
| POST | `/` | 201 + Location | 400 |
| GET | `/?from=&to=&accountId=&categoryId=&page=&size=` | 200 | 400 |
| GET | `/{id}` | 200 | 404 |
| PUT | `/{id}` | 200 | 400, 404 |
| DELETE | `/{id}` | 204 | 404 |

A listagem vem da data mais recente para a mais antiga, com 20 itens por página por padrão e no máximo 100.

### Frontend (Angular)

Tela em `/lancamentos` com filtros por período, conta e categoria, lista paginada e valores coloridos por tipo. O formulário mostra apenas as categorias do tipo escolhido.

### Critérios de aceite

- [x] Migration aplicada pelo Flyway na subida
- [x] Os 5 endpoints conforme a tabela
- [x] RN-01 a RN-07 cobertas por testes
- [x] Teste de isolamento entre usuários passando
- [x] Tela de lançamentos funcionando contra a API real
- [x] Endpoints visíveis no Swagger
- [x] Testes do backend e do frontend passando

---

## T-05 · Saldo e painel

### Objetivo

Mostrar quanto o usuário tem em cada conta e no total, em uma tela inicial com o resumo do mês.

### Depende de

T-04 Lançamentos.

### Regras de negócio

| # | Regra |
|---|---|
| RN-01 | Saldo atual da conta = saldo inicial + receitas − despesas, sem armazenar o valor calculado |
| RN-02 | O saldo pode ser negativo |
| RN-03 | O saldo total soma apenas as contas ativas; contas arquivadas ficam fora |
| RN-04 | Receitas, despesas e resultado do painel consideram só o mês informado |
| RN-05 | O mês vem do cliente, porque "mês atual" depende do fuso horário do usuário |
| RN-06 | O painel mostra os 5 lançamentos mais recentes de todo o histórico |

### Backend

`currentBalance` foi adicionado às respostas de `/api/v1/accounts`, calculado com consulta agrupada, sem uma consulta por conta.

| Método | Rota | Sucesso | Erros |
|---|---|---|---|
| GET | `/api/v1/dashboard?month=YYYY-MM` | 200 | 400 |

### Frontend (Angular)

Painel em `/painel` com saldo total, resumo do mês, contas e últimos lançamentos. A tela de contas ganhou a coluna "Saldo atual".

### Critérios de aceite

- [x] Saldo atual correto após criar, editar e excluir lançamentos
- [x] Saldo total sem contas arquivadas
- [x] Resumo do mês respeitando os limites do período
- [x] Teste de isolamento entre usuários passando
- [x] Painel funcionando contra a API real
- [x] Endpoint visível no Swagger
- [x] Testes do backend e do frontend passando

---

## T-06 · Orçamentos

### Objetivo

Definir um limite mensal por categoria de despesa e acompanhar quanto já foi consumido, com alerta perto de estourar.

### Depende de

T-04 Lançamentos.

### Regras de negócio

| # | Regra |
|---|---|
| RN-01 | Limite obrigatório, maior que zero, com no máximo 2 casas decimais |
| RN-02 | Apenas categorias de despesa, padrão ou do próprio usuário; do contrário responde 400 no campo |
| RN-03 | Um orçamento por categoria em cada mês: repetir responde 409 |
| RN-04 | Gasto = soma das despesas da categoria no mês, em todas as contas, inclusive arquivadas |
| RN-05 | Status: `OK` até 80% do limite, `WARNING` acima de 80% e `EXCEEDED` acima de 100% |
| RN-06 | O status compara os valores exatos, sem o arredondamento do percentual exibido |
| RN-07 | Depois de criado, só o limite pode mudar |
| RN-08 | Orçamento de outro usuário responde 404 |
| RN-09 | Categoria com orçamento não pode ser excluída: responde 409 |

### Backend

Migration `V4__create_budgets.sql`.

Endpoints em `/api/v1/budgets`, todos autenticados:

| Método | Rota | Sucesso | Erros |
|---|---|---|---|
| POST | `/` | 201 + Location | 400, 409 |
| GET | `/?month=YYYY-MM` | 200 | 400 |
| GET | `/{id}` | 200 | 404 |
| PUT | `/{id}` (só o limite) | 200 | 400, 404 |
| DELETE | `/{id}` | 204 | 404 |

### Frontend (Angular)

Tela em `/orcamentos` com seletor de mês, barra de consumo, percentual, valor restante, selo de status e aviso quando há orçamentos em alerta ou estourados.

### Critérios de aceite

- [x] Migration aplicada pelo Flyway na subida
- [x] Os 5 endpoints conforme a tabela
- [x] RN-01 a RN-09 cobertas por testes
- [x] Teste de isolamento entre usuários passando
- [x] Tela de orçamentos funcionando contra a API real
- [x] Endpoints visíveis no Swagger
- [x] Testes do backend e do frontend passando

---

## T-07 · Relatório mensal

### Objetivo

Mostrar como foi o mês: receitas contra despesas, onde o dinheiro foi gasto e o que mudou em relação ao mês anterior.

### Depende de

T-04 Lançamentos.

### Regras de negócio

| # | Regra |
|---|---|
| RN-01 | O relatório cobre um mês e sempre compara com o mês imediatamente anterior, inclusive na virada do ano |
| RN-02 | Receitas, despesas e resultado (receitas − despesas) trazem valor do mês, valor anterior, diferença e variação percentual |
| RN-03 | A variação percentual tem uma casa decimal e usa como base o valor absoluto do mês anterior |
| RN-04 | Quando o mês anterior é zero, a variação percentual é nula, porque não há base de comparação |
| RN-05 | Gastos por categoria incluem as categorias com despesa no mês ou no mês anterior |
| RN-06 | Participação = gasto da categoria ÷ total de despesas do mês, com uma casa decimal; zero quando o mês não tem despesas |
| RN-07 | Categorias ordenadas pelo maior gasto no mês e, no empate, pelo nome |
| RN-08 | Entram os lançamentos de todas as contas, inclusive arquivadas |
| RN-09 | Só entram lançamentos do próprio usuário |

### Backend

Sem migration: o relatório é calculado a partir dos lançamentos.

| Método | Rota | Sucesso | Erros |
|---|---|---|---|
| GET | `/api/v1/reports/monthly?month=YYYY-MM` | 200 | 400, 401 |

Sem o parâmetro `month`, usa o mês atual do servidor.

### Frontend (Angular)

Tela em `/relatorio` com seletor de mês, três cartões (receitas, despesas e resultado) com a variação, gráfico de barras de receitas × despesas dos dois meses, gráfico de barras dos gastos por categoria e tabela com a comparação por categoria.

Os gráficos são feitos em HTML e CSS, sem biblioteca. Subir é bom em receitas e resultado e ruim em despesas; a direção aparece também em texto, não só na cor.

### Critérios de aceite

- [x] Endpoint conforme a tabela
- [x] RN-01 a RN-09 cobertas por testes
- [x] Teste de isolamento entre usuários passando
- [x] Tela do relatório funcionando contra a API real, com estados de carregamento, vazio e erro
- [x] Gráficos legíveis em tela estreita
- [x] Endpoint visível no Swagger
- [x] Testes do backend e do frontend passando

---

## T-08 · Detector de vazamentos

### Objetivo

Mostrar o dinheiro que sai sem o usuário perceber: assinaturas recorrentes, tarifas e juros, e categorias que gastaram acima da própria média. O total aparece como "R$ X vazando este mês".

### Depende de

T-04 Lançamentos.

### Regras de negócio

| # | Regra |
|---|---|
| RN-01 | O detector analisa um mês e usa os três meses anteriores como histórico, inclusive na virada do ano |
| RN-02 | Assinaturas: toda despesa do mês na categoria padrão "Assinaturas" |
| RN-03 | Também é assinatura a cobrança recorrente: mesma descrição (sem diferenciar caixa e espaços) no mês e nos dois meses anteriores, com o total de cada mês anterior a até 10% do total do mês |
| RN-04 | Cobrança recorrente não vale para as categorias padrão "Moradia", "Saúde" e "Educação" (gastos fixos que não são vazamento) nem para "Tarifas e juros" |
| RN-05 | Tarifas e juros: toda despesa do mês na categoria padrão "Tarifas e juros" |
| RN-06 | Assinaturas e tarifas são agrupadas por descrição e ordenadas pelo maior valor; lançamentos sem descrição formam um grupo por categoria |
| RN-07 | Acima da média: categoria cujo gasto do mês passa em mais de 20% da média dos meses com gasto entre os três anteriores; só o excedente (gasto − média) conta como vazamento |
| RN-08 | A média só existe quando a categoria teve gasto em pelo menos dois dos três meses anteriores |
| RN-09 | Nenhum lançamento entra em dois detectores: assinaturas e tarifas ficam fora do cálculo de média |
| RN-10 | As regras por nome valem só para categorias padrão do sistema; categoria do usuário com o mesmo nome não tem tratamento especial |
| RN-11 | Total = assinaturas + tarifas e juros + excedentes acima da média |
| RN-12 | Só entram despesas do próprio usuário, de todas as contas, inclusive arquivadas |

### Backend

Sem migration: os vazamentos são calculados a partir dos lançamentos. As regras ficam em `LeakDetector`, sem acesso a banco.

| Método | Rota | Sucesso | Erros |
|---|---|---|---|
| GET | `/api/v1/leaks?month=YYYY-MM` | 200 | 400, 401 |

Sem o parâmetro `month`, usa o mês atual do servidor.

### Frontend (Angular)

Tela em `/vazamentos` (item "Vazamentos" no menu) com seletor de mês, destaque "R$ X vazando este mês", um cartão com o total de cada detector e uma tabela por detector. Cobranças recorrentes levam o selo "Todo mês".

O painel mostra o card "R$ X vazando este mês" com link para a tela, só quando há vazamento; se a busca falhar, o painel continua funcionando sem o card.

### Critérios de aceite

- [x] Endpoint conforme a tabela
- [x] RN-01 a RN-12 cobertas por testes
- [x] Teste de isolamento entre usuários passando
- [x] Tela de vazamentos funcionando contra a API real, com estados de carregamento, vazio e erro
- [x] Card "R$ X vazando este mês" no painel
- [x] Endpoint visível no Swagger
- [x] Testes do backend e do frontend passando

---

## T-09 · Dados de demonstração

### Objetivo

Disponibilizar seis meses de movimentação financeira para explorar o painel, os relatórios, os orçamentos e os três detectores de vazamentos com o usuário demo do Keycloak.

### Depende de

T-08 Detector de vazamentos.

### Regras

- Usuário demo com ID fixo no realm importado; `DEMO_USER_ID` permite usar o ID de um realm local previamente importado.
- Carga automática exclusiva do perfil `demo`, desativável com `DEMO_SEED_ENABLED=false`.
- Três contas (corrente, poupança e carteira), categoria personalizada Pets, receitas e despesas distribuídas por seis meses, oito orçamentos nos dois meses mais recentes.
- Valores determinísticos em BRL, sem datas futuras; o período termina no mês da primeira carga.
- Exemplos de assinaturas, recorrência fora da categoria Assinaturas, tarifas/juros e lazer acima da média, inclusive no primeiro dia do mês.
- Carga única por usuário, registrada pela migration V5. Marcador e dados pertencem à mesma transação; reinícios não duplicam nem apagam dados.
- Se o usuário já tiver contas ou categorias personalizadas antes da primeira carga, a inicialização é interrompida sem sobrescrevê-las.
- Não há reset automático ou renovação mensal. Uma demonstração nova usa outra base exclusiva e vazia.
- Outros usuários e categorias do sistema são preservados.

### Execução

Consulte [docs/demo.md](docs/demo.md) para configurar o perfil, a base exclusiva e a identidade em instalações existentes do Keycloak. A T-09 usa as telas e endpoints existentes; a publicação fica para T-12.

### Critérios de aceite

- [x] Usuário demo com ID fixo no realm versionado
- [x] Seis meses de dados e resultados dos três detectores verificados com PostgreSQL e MockMvc
- [x] Datas limite (primeiro dia, virada do ano e fevereiro bissexto) cobertas por testes
- [x] Repetição, preservação de alterações, isolamento e rollback cobertos por testes
- [x] Gerador ausente em dev e execução automática desativável
- [x] Guia de execução e comportamento de reinício documentados
- [x] Backend validado com 196 testes sem falhas (mvnw verify)
