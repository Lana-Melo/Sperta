CLIENT_PATH="SpertaClient"
SERVER_PATH="SpertaServer"

OUT_DIR_CLIENT="$CLIENT_PATH/out/"
OUT_DIR_SERVER="$SERVER_PATH/out/"

#Prepare 'database'
rm -rf data/
mkdir -p data/houses data/user_certificates

#Compile
javac -d $OUT_DIR_CLIENT $(find $CLIENT_PATH/src -name "*.java")
javac -d $OUT_DIR_SERVER $(find $SERVER_PATH/src -name "*.java")

#Create Jars
jar cfe SpertaClient.jar sperta.SpertaClient -C $OUT_DIR_CLIENT .
jar cfe SpertaServer.jar sperta.SpertaServer -C $OUT_DIR_SERVER .