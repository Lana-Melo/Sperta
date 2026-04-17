CLIENT_PATH="SpertaClient"
SERVER_PATH="SpertaServer"

OUT_DIR_CLIENT="$CLIENT_PATH/out/"
OUT_DIR_SERVER="$SERVER_PATH/out/"

# --- Client setup
rm    -rf  $CLIENT_PATH/data
mkdir -p   $CLIENT_PATH/data

# --- Server setup
#Prepare 'database'
rm    -rf $SERVER_PATH/data
mkdir -p  $SERVER_PATH/data/houses $SERVER_PATH/data/user_certificates


# --- General
#Compile
javac -d $OUT_DIR_CLIENT $(find $CLIENT_PATH/src -name "*.java")
javac -d $OUT_DIR_SERVER $(find $SERVER_PATH/src -name "*.java")

#Create Jars
jar cfe $CLIENT_PATH/SpertaClient.jar sperta.SpertaClient -C $OUT_DIR_CLIENT .
jar cfe $SERVER_PATH/SpertaServer.jar sperta.SpertaServer -C $OUT_DIR_SERVER .

#Importing the SpertaClient.jar to Server/out
cp $CLIENT_PATH/SpertaClient.jar $OUT_DIR_SERVER/