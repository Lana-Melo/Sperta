SCRIPT_DIR=scripts
ATTESTATION_TXT_PATH=SpertaServer/security/attestation.txt
PASSWORD=changeit

java -cp "$SCRIPT_DIR" EncryptAttestationPath "$ATTESTATION_TXT_PATH" "$PASSWORD"
