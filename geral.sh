bash scripts/compile_n_clean.sh

bash scripts/users_truststores_clean.sh

rm -rf SpertaServer/security/attestation.txt
echo -n "SpertaServer/out/SpertaClient.jar" > SpertaServer/security/attestation.txt
bash scripts/encrypt_attestation_txt.sh

