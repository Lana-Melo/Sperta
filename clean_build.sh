# TODO - THIS FILE WASN'T TESTED AT ALL
SCRIPT_DIR=scripts

# Cleans trust stores and keys
bash $SCRIPT_DIR/truststores_clean.sh


# Cleans attestation.txt and writes on it the path to the attestation file
rm SpertaServer/security/attestation.txt
echo "SpertaServer/out/SpertaClient.jar" > SpertaServer/security/attestation.txt
bash $SCRIPT_DIR/encrypt_attestation_txt.sh

# Runs build.sh
bash build.sh