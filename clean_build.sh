SCRIPT_DIR=scripts

# Cleans attestation.txt and writes on it the path to the attestation file
rm SpertaServer/security/attestation.txt
echo -n "SpertaServer/out/SpertaClient.jar" > SpertaServer/security/attestation.txt
bash $SCRIPT_DIR/encrypt_attestation_txt.sh

# Cleans the truststores
bash $SCRIPT_DIR/users_truststores_clean.sh

# Builds the project
bash $SCRIPT_DIR/compile_n_clean.sh