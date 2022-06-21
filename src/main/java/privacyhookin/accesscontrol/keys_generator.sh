#!/bin/bash
# Generate key pair for request authorization
ssh-keygen -t rsa -b 4096 -E SHA512 -f jwtRS512.key
openssl rsa -in jwtRS512.key -pubout -outform PEM -out jwtRS512.key.pub