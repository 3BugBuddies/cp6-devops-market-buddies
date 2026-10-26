#!/usr/bin/env bash
#
# VERIFIQUE SE ESTA COM A ASSINATURA CORRETA
#
# az account show
# az account list -o table
# az account set --subscription "nome-da-assinatura-correta"
#
# Rodar UMA vez, fora da pipeline (terminal ou Cloud Shell com Bash).
# Os segredos (senha do banco, JWT, e-mail) NÃO ficam aqui: são gravados no Web App
# pela pipeline, a partir das variáveis secretas cadastradas no Azure DevOps.

set -euo pipefail

# Variáveis
RESOURCE_GROUP_NAME="rg-cp6-marketbuddies"
LOCATION="brazilsouth"
APP_SERVICE_PLAN="plan-cp6-marketbuddies"
WEBAPP_NAME="marketbuddies-rm563925"
RUNTIME="JAVA:21-java21"
APP_INSIGHTS_NAME="ai-cp6-marketbuddies"
SQL_SERVER_NAME="sql-marketbuddies-rm563925"
SQL_DB_NAME="marketbuddies"
SQL_ADMIN_USER="mbadmin"

# Senha do admin do SQL Server: digitada na hora, nunca escrita no arquivo
# (mínimo 8 caracteres, com maiúscula, minúscula, número e símbolo)
read -r -s -p "Senha do admin do SQL Server ($SQL_ADMIN_USER): " SQL_ADMIN_PASSWORD
echo

# Registrar os Serviços utilizados
az provider register --namespace Microsoft.Web
az provider register --namespace Microsoft.Insights
az provider register --namespace Microsoft.OperationalInsights
az provider register --namespace Microsoft.Sql

# Registrar a extensão utilizada
az extension add --name application-insights --upgrade --only-show-errors

# Criação do Grupo de Recursos
az group create \
  --name $RESOURCE_GROUP_NAME \
  --location "$LOCATION"

# Criar Application Insights
az monitor app-insights component create \
  --app $APP_INSIGHTS_NAME \
  --location "$LOCATION" \
  --resource-group $RESOURCE_GROUP_NAME \
  --application-type web

# Criar o Servidor SQL (ponto de administração do banco)
az sql server create \
  --name $SQL_SERVER_NAME \
  --resource-group $RESOURCE_GROUP_NAME \
  --location "$LOCATION" \
  --admin-user $SQL_ADMIN_USER \
  --admin-password "$SQL_ADMIN_PASSWORD" \
  --enable-public-network true

# Liberar o acesso dos serviços da Azure (0.0.0.0 = IPs internos da Azure, inclui o Web App)
az sql server firewall-rule create \
  --resource-group $RESOURCE_GROUP_NAME \
  --server $SQL_SERVER_NAME \
  --name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0

# Liberar o IP de quem roda o script (para o Query Editor do portal e o terminal)
# No Cloud Shell, este é o IP do Cloud Shell, não o da sua máquina
MEU_IP=$(curl -s https://api.ipify.org)
az sql server firewall-rule create \
  --resource-group $RESOURCE_GROUP_NAME \
  --server $SQL_SERVER_NAME \
  --name AllowMeuIP \
  --start-ip-address "$MEU_IP" \
  --end-ip-address "$MEU_IP"

# Criar o Banco de Dados (Basic, 5 DTU). As tabelas são criadas pelo próprio app (Hibernate)
az sql db create \
  --resource-group $RESOURCE_GROUP_NAME \
  --server $SQL_SERVER_NAME \
  --name $SQL_DB_NAME \
  --edition Basic \
  --backup-storage-redundancy Local \
  --zone-redundant false

# Criar o Plano de Serviço
az appservice plan create \
  --name $APP_SERVICE_PLAN \
  --resource-group $RESOURCE_GROUP_NAME \
  --location "$LOCATION" \
  --sku B1 \
  --is-linux

# Criar o Serviço de Aplicativo
az webapp create \
  --name $WEBAPP_NAME \
  --resource-group $RESOURCE_GROUP_NAME \
  --plan $APP_SERVICE_PLAN \
  --runtime "$RUNTIME"

# Manter o app sempre carregado (o job de entrega roda em segundo plano)
az webapp config set \
  --name $WEBAPP_NAME \
  --resource-group $RESOURCE_GROUP_NAME \
  --always-on true

# Guardar os logs da aplicação (stdout/stderr) para o Log stream
az webapp log config \
  --name $WEBAPP_NAME \
  --resource-group $RESOURCE_GROUP_NAME \
  --docker-container-logging filesystem

# Recuperar a String de Conexão do Application Insights
CONNECTION_STRING=$(az monitor app-insights component show \
  --app $APP_INSIGHTS_NAME \
  --resource-group $RESOURCE_GROUP_NAME \
  --query connectionString \
  --output tsv)

# Configurar as Variáveis de Ambiente que NÃO são segredo
# (banco, JWT, senha do admin e e-mail são gravados pela pipeline, a partir das variáveis secretas)
az webapp config appsettings set \
  --name "$WEBAPP_NAME" \
  --resource-group "$RESOURCE_GROUP_NAME" \
  --settings \
    APPLICATIONINSIGHTS_CONNECTION_STRING="$CONNECTION_STRING" \
    ApplicationInsightsAgent_EXTENSION_VERSION="~3" \
    XDT_MicrosoftApplicationInsights_Mode="Recommended" \
    XDT_MicrosoftApplicationInsights_PreemptSdk="1" \
    TZ="America/Sao_Paulo" \
    WEBSITES_CONTAINER_START_TIME_LIMIT="1800" \
    APP_BASE_URL="https://$WEBAPP_NAME.azurewebsites.net"

# Reiniciar o Web App
az webapp restart --name $WEBAPP_NAME --resource-group $RESOURCE_GROUP_NAME

# Criar a conexão do nosso Web App com o Application Insights
az monitor app-insights component connect-webapp \
    --app $APP_INSIGHTS_NAME \
    --web-app $WEBAPP_NAME \
    --resource-group $RESOURCE_GROUP_NAME

# Dados para cadastrar na pipeline (Pipelines > Edit > Variables)
echo
echo "Infra criada. Cadastre na pipeline:"
echo "  DB_URL  = jdbc:sqlserver://$SQL_SERVER_NAME.database.windows.net:1433;database=$SQL_DB_NAME;encrypt=true;trustServerCertificate=false;loginTimeout=30"
echo "  DB_USER = $SQL_ADMIN_USER"
echo "Variáveis secretas (marcar 'Keep this value secret'):"
echo "  DB_PASSWORD, JWT_SECRET, ADMIN_PASSWORD, MAIL_USERNAME, MAIL_PASSWORD"
echo "App: https://$WEBAPP_NAME.azurewebsites.net"
