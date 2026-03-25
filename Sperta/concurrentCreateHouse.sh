#!/bin/bash
#Este script testa a concorrênccia na criação de uma casa com o mesmo nome e o
#resultado esperado é que apenas um user consiga
#!/bin/bash

SERVER="localhost"
USERS=("lana" "sofia" "gui" "yan" "bruno" "henrique" "leonor" "bidoia")
HOUSE="CasaComMesmoNome2"

TMP_FILE=$(mktemp)

echo "A iniciar teste de concorrência..."

run_client() {
  USER=$1

  (
    RESULT=$(
      {
        sleep 0.2
        echo "CREATE $HOUSE"
        sleep 0.2
      } | java -jar SpertaClient.jar $SERVER $USER 1234 2>/dev/null
    )

    echo "$RESULT" >> "$TMP_FILE"
  ) &
}

# Launch all users in parallel
for USER in "${USERS[@]}"
do
  run_client "$USER"
done

# Wait for all background jobs
wait

# Count results
OK_COUNT=$(grep -o "Response: OK" "$TMP_FILE" | wc -l)
NOK_COUNT=$(grep -o "Response: NOK" "$TMP_FILE" | wc -l)

echo "--------------------------------------"
echo "Resultados:"
echo "OKs:  $OK_COUNT"
echo "NOKs: $NOK_COUNT"
echo "--------------------------------------"
echo "✅ Teste concluído!"

# Clean up temp file
rm "$TMP_FILE"