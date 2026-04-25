SCRIPT_DIR=scripts
ATTESTATION_TXT_PATH=SpertaServer/security/attestation.txt
PASSWORD=changeit

javac -d "$SCRIPT_DIR" $SCRIPT_DIR/EncryptAttestationPath.java
java -cp "$SCRIPT_DIR" EncryptAttestationPath "$ATTESTATION_TXT_PATH" "$PASSWORD"
