#!/usr/bin/env bash
#
# VERIFIQUE SE ESTA COM A ASSINATURA CORRETA
#
# az account show
#
# Apaga TODOS os recursos da CP6 (Web App, plano, App Insights, SQL Server e banco).
# Rodar só depois da correção.

set -euo pipefail

RESOURCE_GROUP_NAME="rg-cp6-marketbuddies"

read -r -p "Apagar o grupo de recursos $RESOURCE_GROUP_NAME e tudo dentro dele? (digite sim): " CONFIRMA
if [ "$CONFIRMA" != "sim" ]; then
  echo "Cancelado."
  exit 0
fi

# Excluir o Grupo de Recursos (roda em segundo plano na Azure)
az group delete \
  --name $RESOURCE_GROUP_NAME \
  --yes \
  --no-wait

echo "Exclusão iniciada. Acompanhe com: az group show --name $RESOURCE_GROUP_NAME"
