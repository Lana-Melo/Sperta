# script para executar quando quisermos resetar as truststores dos users

CLIENT_USERS_SECURITY_LIB_PATH=SpertaClient/security/users

echo "Cleaning user truststores..."

for user_dir in "$CLIENT_USERS_SECURITY_LIB_PATH"/*; do
  if [ -d "$user_dir" ]; then
    echo "Checking $user_dir..."

    # remove qualquer ficheiro com 'truststore' no nome
    find "$user_dir" -type f -name "*truststore*" -exec rm -f {} \;

    echo "Cleaned truststores in $user_dir"
  fi
done

echo "Done."