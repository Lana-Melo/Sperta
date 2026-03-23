#!/bin/bash

SERVER="localhost"
PORT="22345"

OWNER="lana"
HOUSE="EC_Concorrente"

MAINTAINERS=("lana" "sofia" "gui" "yan" "bruno" "henroque" "leonor")

USERS=("lana" "sofia" "gui" "yan" "bruno" "henroque" "leonor" "bidoia" )

echo "🚀 Setup inicial (owner: $OWNER)"

# 1. Criar casa, dispositivo e permissões
SETUP_RESULT=$(
{
  sleep 0.2
  echo "CREATE $HOUSE"
  sleep 0.2
  echo "RD $HOUSE P"
  sleep 0.2

  for u in "${MAINTAINERS[@]}"
  do
    echo "ADD $u $HOUSE P"
    sleep 0.1
  done

  sleep 0.2
} | java -jar SpertaClient.jar $SERVER $OWNER 123 2>/dev/null
)

echo "✅ Setup concluído"
echo "--------------------------------------"


# Função para executar EC em paralelo
run_client() {
  USER=$1

  (
    RESULT=$(
      {
        sleep 0.2
        echo "EC $HOUSE P1 100"
        sleep 0.2
      } | java -jar SpertaClient.jar $SERVER $USER 123 2>/dev/null
    )

    # Filtrar apenas OK/NOK
    LAST_LINE=$(echo "$RESULT" | grep -E "^(OK|NOK|NOPERM|NOHM|NOD)$" | tail -n 1)

    echo $RESULT

  ) &
}

echo "⚡ A executar EC concorrente..."

# 2. Executar EC em paralelo
for USER in "${USERS[@]}"
do
  run_client "$USER"
done

wait

echo "--------------------------------------"
echo "✅ Teste concluído!"