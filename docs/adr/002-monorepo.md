# ADR-002: Monorepo para API e aplicação web

- Status: aceito
- Data: 2026-11-09

## Contexto

As tarefas do MVP entregam uma parte da API e sua tela Angular. Manter front e back no mesmo repositório facilita revisar mudanças verticais e executar o ambiente completo.

## Decisão

Usar um monorepo com `server/` para Spring Boot, `web/` para Angular e `docker/` para os arquivos do Keycloak. O Compose fica na raiz para subir as dependências locais.

## Consequências

Uma alteração de funcionalidade pode conter contrato, implementação, tela e testes no mesmo commit. Os builds continuam independentes e cada parte mantém suas ferramentas próprias.
