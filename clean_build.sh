# TODO

# Cleans trust stores and keys

# Cleans attestation.txt and writes on it the path to the attestation file
rm SpertaServer/security/attestation.txt
echo "SpertaServer/out/SpertaClient.jar" > SpertaServer/security/attestation.txt
bash scripts/encrypt_attestation_txt.sh

# Runs build.sh
bash build.sh