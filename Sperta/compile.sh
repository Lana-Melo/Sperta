rm -r data/
mkdir data
mkdir data/houses
javac -d out $(find src -name "*.java")
jar cfe SpertaServer.jar sperta.SpertaServer -C out .
jar cfe SpertaClient.jar sperta.SpertaClient -C out .

stat -c '%n:%s' SpertaClient.jar > data/attestation.txt