CLIENT_SERCURITY_LIB_PATH=SpertaClient/security
CLIENT_USERS_SECURITY_LIB_PATH=$CLIENT_SERCURITY_LIB_PATH/users
PASS=changeit
#colocar aqui o IP do servidor
SERVER_IP=localhost

java -jar SpertaClient/SpertaClient.jar $SERVER_IP:22345 \
    $CLIENT_USERS_SECURITY_LIB_PATH/gui/truststore.gui $PASS \
    $CLIENT_USERS_SECURITY_LIB_PATH/gui/keystore.gui $PASS \
    gui 12345