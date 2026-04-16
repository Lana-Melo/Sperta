mkdir -p client/users/lana
mkdir -p client/users/sofia
mkdir -p client/users/gui

STORE_PASS=SCFase2Grupo07
#deve dar pra automatizar isto da pass pra n tarmos smepre a escrever depois vejo como se faz isso 

# ---- SERVIDOR ----
keytool -genkeypair -alias myServer -keyalg RSA -keysize 2048 -keystore server/keystore.server 

# exportar cert do servidor para dar aos clientes
keytool -exportcert -alias myServer -file server/certServer.cer -keystore server/keystore.server 

#-------------------------------------------------------------------------------------------------------------
# ---- lana ----
# gerar par de chaves (keystore já contém o cert do user1)
keytool -genkeypair -alias lana -keyalg RSA -keysize 2048 -keystore client/users/lana/keystore.lana 

# exportar cert do lana (para enviar ao servidor no login)
keytool -exportcert -alias lana -file client/users/lana/lana.cer -keystore client/users/lana/keystore.lana

# truststore começa só com o cert do servidor (certs de outros users adicionados dinamicamente)
# estou a por o certificado do server na minha truststore pk quero confiar nele :)
keytool -importcert -alias myServer -file server/certServer.cer -keystore client/users/lana/truststore.lana

#--------------------------------------------------------------------------------------------------------------
# ---- sofia ----
keytool -genkeypair -alias sofia -keyalg RSA -keysize 2048 -keystore client/users/sofia/keystore.sofia 

keytool -exportcert -alias sofia -file client/users/sofia/sofia.cer -keystore client/users/sofia/keystore.sofia

# truststore começa só com o cert do servidor (certs de outros users adicionados dinamicamente)
# estou a por o certificado do server na minha truststore pk quero confiar nele :)
keytool -importcert -alias myServer -file server/certServer.cer -keystore client/users/sofia/truststore.sofia

#--------------------------------------------------------------------------------------------------------------
# ---- gui ----
keytool -genkeypair -alias gui -keyalg RSA -keysize 2048 -keystore client/users/gui/keystore.gui 

keytool -exportcert -alias gui -file client/users/gui/gui.cer -keystore client/users/gui/keystore.gui

# truststore começa só com o cert do servidor (certs de outros users adicionados dinamicamente)
# estou a por o certificado do server na minha truststore pk quero confiar nele :)
keytool -importcert -alias myServer -file server/certServer.cer -keystore client/users/gui/truststore.gui