PASS=changeit
SERVER_SECURITY_LIB_PATH=SpertaServer/security
CLIENT_SERCURITY_LIB_PATH=SpertaClient/security
CLIENT_USERS_SECURITY_LIB_PATH=$CLIENT_SERCURITY_LIB_PATH/users

mkdir -p $CLIENT_USERS_SECURITY_LIB_PATH/lana
mkdir -p $CLIENT_USERS_SECURITY_LIB_PATH/sofia
mkdir -p $CLIENT_USERS_SECURITY_LIB_PATH/gui
mkdir -p $SERVER_SECURITY_LIB_PATH

#deve dar pra automatizar isto da pass pra n tarmos smepre a escrever depois vejo como se faz isso 

# ---- SERVIDOR ----
keytool -genkeypair -alias myServer -keyalg RSA -keysize 2048 \
        -keystore  $SERVER_SECURITY_LIB_PATH/keystore.server \
        -storetype JCEKS \
        -storepass $PASS -keypass $PASS  -dname "CN=Server"

# exportar cert do servidor para dar aos clientes
keytool -exportcert -alias myServer -file $SERVER_SECURITY_LIB_PATH/certServer.cer \
        -keystore $SERVER_SECURITY_LIB_PATH/keystore.server -storepass $PASS

# ---- CLIENT ---- 
# importar cert do servidor
keytool -importcert -alias myServer -file $SERVER_SECURITY_LIB_PATH/certServer.cer \
        -keystore $CLIENT_SERCURITY_LIB_PATH/truststore.client -storepass $PASS -noprompt

#-------------------------------------------------------------------------------------------------------------
# ---- USERS ----
# ---- lana ----
# gerar par de chaves (keystore já contém o cert do user1)
keytool -genkeypair -alias lana -keyalg RSA -keysize 2048 \
        -keystore $CLIENT_USERS_SECURITY_LIB_PATH/lana/keystore.lana \
        -storetype JCEKS \
        -storepass $PASS -keypass $PASS -dname "CN=lana"

# exportar cert do lana (para enviar ao servidor no login)
keytool -exportcert -alias lana -file $CLIENT_USERS_SECURITY_LIB_PATH/lana/lana.cer \
        -keystore $CLIENT_USERS_SECURITY_LIB_PATH/lana/keystore.lana -storepass $PASS

#--------------------------------------------------------------------------------------------------------------
# ---- sofia ----
keytool -genkeypair -alias sofia -keyalg RSA -keysize 2048 \
        -keystore  $CLIENT_USERS_SECURITY_LIB_PATH/sofia/keystore.sofia \
        -storetype JCEKS \
        -storepass $PASS -keypass $PASS -dname "CN=sofia"

keytool -exportcert -alias sofia -file  $CLIENT_USERS_SECURITY_LIB_PATH/sofia/sofia.cer \
        -keystore  $CLIENT_USERS_SECURITY_LIB_PATH/sofia/keystore.sofia -storepass $PASS

#--------------------------------------------------------------------------------------------------------------
# ---- gui ----
keytool -genkeypair -alias gui -keyalg RSA -keysize 2048 \
        -keystore  $CLIENT_USERS_SECURITY_LIB_PATH/gui/keystore.gui \
        -storetype JCEKS \
        -storepass $PASS -keypass $PASS -dname "CN=gui"

keytool -exportcert -alias gui -file $CLIENT_USERS_SECURITY_LIB_PATH/gui/gui.cer \
        -keystore $CLIENT_USERS_SECURITY_LIB_PATH/gui/keystore.gui -storepass $PASS
