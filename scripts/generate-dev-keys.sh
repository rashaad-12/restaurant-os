#!/usr/bin/env sh

set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PRIVATE="$ROOT/auth-service/src/main/resources/keys/app_private.pem"
PUBLIC="$ROOT/core-security/src/main/resources/keys/app_public.pem"

mkdir -p "$(dirname "$PRIVATE")" "$(dirname "$PUBLIC")"

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$PRIVATE"
openssl rsa -pubout -in "$PRIVATE" -out "$PUBLIC"

echo "Generated dev keypair:"
echo "private (signer): $PRIVATE"
echo "public (verifier): $PUBLIC"
