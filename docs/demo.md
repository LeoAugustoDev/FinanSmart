# Dados de demonstração (T-09)

O perfil `demo` carrega três contas, a categoria personalizada Pets, oito orçamentos e seis meses de receitas/despesas. O período termina no mês da primeira carga. Não há lançamentos futuros. Os exemplos incluem assinaturas, tarifas, uma cobrança recorrente e lazer acima da média, inclusive se a carga ocorrer no primeiro dia do mês.

## Executar localmente

1. Inicie Postgres e Keycloak com `docker compose up -d` na raiz do repositório.
2. Use uma base exclusiva para a demonstração. Por exemplo, crie-a uma vez:

   ```powershell
   docker compose exec postgres psql -U finansmart -d finansmart -c 'CREATE DATABASE finansmart_demo;'
   ```

3. No terminal da pasta `server`, configure e execute:

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = 'demo'
   $env:DB_URL = 'jdbc:postgresql://localhost:5432/finansmart_demo'
   $env:DB_USERNAME = 'finansmart'
   $env:DB_PASSWORD = 'finansmart'
   $env:KEYCLOAK_ISSUER_URI = 'http://localhost:8180/realms/finansmart'
   $env:CORS_ALLOWED_ORIGINS = 'http://localhost:4200'
   .\mvnw.cmd spring-boot:run
   ```

4. Inicie o Angular em `web` com `npm start`. Entre com `demo` / `demo` e confira Painel, Lançamentos, Orçamentos, Relatório e Vazamentos.

As credenciais acima são apenas as do ambiente local. A configuração de hospedagem fica para T-12.

## Identidade do Keycloak

O realm versionado define o usuário demo com ID `7d9f3c1e-5a2b-4e8f-9c6d-1b2a3c4d5e6f`. O backend usa esse mesmo valor como proprietário dos dados.

Um realm já importado não é atualizado automaticamente pelo `--import-realm`. Nesse caso, consulte o ID do usuário demo em Users no console do Keycloak e configure `$env:DEMO_USER_ID = 'id-existente'` antes da primeira carga. Não é necessário excluir o realm, usuários ou volumes. Novas instalações usam o ID fixo sem configuração adicional.

## Repetição e preservação

- Somente o perfil `demo` disponibiliza o gerador. `DEMO_SEED_ENABLED=false` desliga sua execução automática.
- A migration V5 registra a primeira carga por usuário. O marcador e os dados são gravados na mesma transação; execuções simultâneas não duplicam o conteúdo.
- Reiniciar preserva IDs, lançamentos e alterações feitas pelo visitante. Não há reset automático nem atualização mensal automática do histórico.
- Se o usuário já possuir contas ou categorias personalizadas sem uma carga registrada, a inicialização falha com uma mensagem explícita; os dados existentes são preservados. Use uma base demo vazia.
- Para uma demonstração nova em outra data, configure outra base exclusiva e vazia. Nunca apague o banco de desenvolvimento para renovar a demo.
- Dados de outros usuários e categorias do sistema são preservados.

## Validação

Os testes usam PostgreSQL via Testcontainers, verificam seis meses (incluindo virada de ano e fevereiro bissexto), valores positivos, ausência de datas futuras, resultados dos três detectores, isolamento, repetição sem duplicação e rollback em conflito com dados existentes.