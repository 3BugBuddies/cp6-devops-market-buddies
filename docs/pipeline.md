# Pipeline e infraestrutura

## Ordem de execução

1. **Infraestrutura (uma vez).** No terminal com `az login` feito (ou no Cloud Shell, Bash), na assinatura certa:
   ```bash
   bash infra/infra-app.sh
   ```
   O script pede a senha do admin do SQL Server (não ecoa nem grava) e cria: grupo de recursos,
   Application Insights, SQL Server + banco `marketbuddies` (Basic), plano B1 Linux e o Web App Java 21.
   No fim, imprime a `DB_URL` e a lista das variáveis secretas.
2. **Service connection.** Azure DevOps > Project settings > Service connections > New > Azure Resource Manager,
   escopo na **assinatura**, nome `sc-marketbuddies`.
3. **Pipeline.** Pipelines > New pipeline > Azure Repos Git > repositório > *Existing Azure Pipelines YAML file*
   > `/azure-pipeline.yml`.
4. **Variáveis.** Na pipeline, Edit > Variables: cadastrar as variáveis da tabela abaixo, marcando
   *Keep this value secret* nas secretas.
5. **Run pipeline.** O estágio CI compila e roda os testes (aba *Tests*); o CD grava as variáveis no Web App e
   publica o JAR. O environment `marketbuddies-prod` guarda o histórico de deploys. Depois disso, todo merge
   na `main` dispara a pipeline sozinho.

Para apagar tudo depois da correção: `bash infra/destroy.sh`.

## Variáveis

| Nome | Secreta? | De onde vem |
|---|---|---|
| `DB_URL` | não | impressa no fim do `infra-app.sh` |
| `DB_USER` | não | `mbadmin` (admin do SQL Server) |
| `DB_PASSWORD` | sim | senha digitada ao rodar o `infra-app.sh` |
| `JWT_SECRET` | sim | texto aleatório com 32+ caracteres (ex.: `openssl rand -base64 48`) |
| `ADMIN_PASSWORD` | sim | senha do admin da loja (usuário `admin@marketbuddies.com`) |
| `MAIL_USERNAME` | sim | conta Gmail que envia os e-mails |
| `MAIL_PASSWORD` | sim | senha de app do Gmail |
| `DDL_AUTO` | não | fixa no YAML: `update` (o Hibernate cria e atualiza as tabelas) |
| `APP_BASE_URL`, `TZ`, App Insights | não | gravadas pelo `infra-app.sh` |

As variáveis de pipeline viram *app settings* do Web App pela tarefa `AzureAppServiceSettings@1`, e o
`application.yaml` as lê como variáveis de ambiente.
