#!/bin/bash
#Este script testa a concorrênccia na criação de uma casa com o mesmo nome e o
#resultado esperado é que apenas um user consiga
SERVER="localhost"
PORT="22345"
USERS=("lana" "sofia" "gui" "yan" "bruno" "henroque" "leonor" "bidoia" )
HOUSE="bondeDaLana"

echo "A iniciar teste de concorrência..."

# Função para executar comandos num cliente

run_client() {
  USER=$1

  (
    RESULT=$(
      {
        sleep 0.2
        echo "CREATE $HOUSE"
        sleep 0.2
      } | java -jar SpertaClient.jar $SERVER $USER 123 2>/dev/null
    )

    echo $RESULT
  ) &
}

# Lançar todos os utilizadores em paralelo
for USER in "${USERS[@]}"
do
  run_client "$USER"
done

# Esperar todos terminarem
wait

echo "--------------------------------------"
echo "✅ Teste concluído!"

