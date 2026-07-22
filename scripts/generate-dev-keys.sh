#!/usr/bin/env sh

set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KEYS="$ROOT/.env/dev/keys"

PRIVATE="$KEYS/app_private.pem"
PUBLIC="$KEYS/app_public.pem"

mkdir -p "$KEYS"

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$PRIVATE"
openssl rsa -pubout -in "$PRIVATE" -out "$PUBLIC"

echo "Generated dev keypair:"
echo "private (signer): $PRIVATE"
echo "public (verifier): $PUBLIC"
