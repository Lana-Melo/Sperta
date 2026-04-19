CLIENT_SERCURITY_LIB_PATH=SpertaClient/security
CLIENT_USERS_SECURITY_LIB_PATH=$CLIENT_SERCURITY_LIB_PATH/users
PASS=changeit

java -jar SpertaClient/SpertaClient.jar localhost:22345 \
    $CLIENT_USERS_SECURITY_LIB_PATH/lana/truststore.lana $PASS \
    $CLIENT_USERS_SECURITY_LIB_PATH/lana/keystore.lana $PASS \
    lana 12345