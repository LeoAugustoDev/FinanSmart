# ADR-001: Autenticação delegada ao Keycloak

- Status: aceito
- Data: 2026-11-09

## Contexto

O FinanSmart precisa de login, cadastro, logout, recuperação de sessão e uma futura entrada social pelo Google. Criar senhas e tokens dentro da API aumentaria o código sensível que o projeto precisaria manter.

## Decisão

Usar Keycloak como provedor de identidade. O Angular autentica com Authorization Code e PKCE em um cliente público. A API valida os JWTs como OAuth2 Resource Server e usa o claim `sub` como `user_id`.

## Consequências

O FinanSmart não armazena senhas nem possui tabela de usuários. O ambiente local precisa do Keycloak. O realm importado concentra cliente, tema, usuário demo e configuração do Google. Em produção, o Keycloak deve usar `start`, HTTPS e secrets fornecidos pelo ambiente.
