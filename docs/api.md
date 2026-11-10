# API FinanSmart

Com a API local em execução, abra http://localhost:8080/swagger-ui/index.html.
O contrato JSON está em http://localhost:8080/v3/api-docs.

Use Authorize com o access token emitido pelo Keycloak. A API não emite tokens.
O token identifica o proprietário; não envie userId no corpo. Tokens expirados
retornam 401. Recursos de outro usuário retornam 404.

## Exemplo: criar conta

POST `/api/v1/accounts`, Content-Type `application/json`:

```json
{"name":"Conta principal","type":"CHECKING","initialBalance":1500.00}
```

A resposta 201 contém a conta criada e o cabeçalho Location. Use o ID recebido
nas operações seguintes. Os tipos aceitos e os campos obrigatórios aparecem
nos schemas do Swagger. Valores monetários são decimais em BRL.

## Consultas mensais

- `/api/v1/dashboard?month=2026-11`: saldo e resumo do mês.
- `/api/v1/reports/monthly?month=2026-11`: comparação com o mês anterior.
- `/api/v1/leaks?month=2026-11`: assinaturas, tarifas e excedentes sobre a média.

Datas usam YYYY-MM-DD e meses YYYY-MM. Na comparação mensal, o percentual é
nulo quando a base anterior é zero. Vazamentos são indícios calculados pelas
regras documentadas em TASKS.md, não classificações definitivas de desperdício.

## Erros

As validações e regras de negócio usam `application/problem+json`, com status,
title e detail; errors contém mensagens por campo quando aplicável. Exemplo:

```json
{"type":"about:blank","title":"Bad Request","status":400,"detail":"Dados inválidos","errors":{"name":"não deve estar em branco"}}
```

400 indica entrada inválida; 403 impede alterar categorias do sistema; 404 indica
recurso indisponível para o usuário; 409 indica conflito de unicidade, vínculo
ou edição concorrente. A resposta 401 é produzida pela segurança e pode não ter corpo.
