#!/bin/bash
# -- Generate key pair for request authorization
# declare an array variable
declare -a arr=("client" "driver" "order" "restaurant" "routing")
for i in "${!arr[@]}"; do
  # Generate a 2048-bit RSA private key
  openssl genrsa -out private_key_"${arr[i]}".pem 2048
  # Convert private Key to PKCS#8 format (readable by Java)
  # openssl pkcs8 -topk8 -inform PEM -outform DER -in private_key_"${arr[i]}".pem -out private_key_"${arr[i]}".der -nocrypt
  openssl pkcs8 -topk8 -inform PEM -outform DER -in private_key_"${arr[i]}".pem -out ../../../../../delivery/src/main/resources/privateKeys/private_key_"${arr[i]}".der -nocrypt
  # Output public key portion in DER format (readable by Java)
  openssl rsa -in private_key_"${arr[i]}".pem -pubout -outform DER -out ../../../../../hook-in/src/main/resources/publicKeys/public_key_"${arr[i]}".der
done
