rm -r data/
mkdir data
mkdir data/houses
javac -d out $(find src -name "*.java")
jar cfe SpertaServer.jar sperta.SpertaServer -C out .
jar cfe SpertaClient.jar sperta.SpertaClient -C out .

if stat -c '%n:%s' SpertaClient.jar >/dev/null 2>&1; then
    stat -c '%n:%s' SpertaClient.jar > data/attestation.txt
else
    stat -f '%N:%z' SpertaClient.jar > data/attestation.txt
fi