# ADR-004: Entrega vertical por tarefa

- Status: aceito
- Data: 2026-11-09

## Contexto

Construir toda a API antes das telas deixaria a integração para o final e aumentaria o risco de descobrir problemas tarde.

## Decisão

Cada tarefa do MVP entrega uma fatia funcional: endpoints, regras, tela e testes relacionados. As branches seguem `feat/T-XX-nome`.

## Consequências

Cada branch pode ser revisada e demonstrada isoladamente. O trabalho exige manter o contrato entre API e Angular atualizado durante cada tarefa.
