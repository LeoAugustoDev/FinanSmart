# ADR-003: Valores monetários com BigDecimal

- Status: aceito
- Data: 2026-11-09

## Contexto

Saldo, lançamentos, orçamentos e relatórios precisam preservar centavos. Tipos binários de ponto flutuante podem gerar arredondamentos inadequados para dinheiro.

## Decisão

Usar `BigDecimal` na API e `NUMERIC(19,2)` no PostgreSQL. O front formata os valores como BRL e impede entradas numéricas inválidas.

## Consequências

Operações e percentuais definem escala e arredondamento explicitamente. O contrato da API transporta números decimais e a moeda do produto é BRL.
