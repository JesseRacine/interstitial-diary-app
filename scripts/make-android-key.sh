#!/bin/bash
# Makes the Android signing key (once) and stores it as this repo's GitHub Actions secrets.
#
# Run it yourself in a terminal, from anywhere inside this repo:   bash scripts/make-android-key.sh
# It asks for a password, writes the key to ~/interstitial-diary-keys/interstitial.p12 (outside every repo), and sets
# the secrets ANDROID_KEYSTORE_BASE64, ANDROID_KEYSTORE_PASSWORD and ANDROID_KEY_ALIAS with `gh`.
#
# Keep that file AND the password somewhere safe (a password manager, a USB stick). Android only installs an update
# signed with the same key: lose either one and every phone has to uninstall the app before it can install a new build.
set -euo pipefail

ALIAS=interstitial
DIR="${KEY_DIR:-$HOME/interstitial-diary-keys}"
KEY="$DIR/interstitial.p12"

cd "$(dirname "$0")/.."
command -v openssl >/dev/null || { echo "openssl is not installed."; exit 1; }
if [ -z "${SKIP_UPLOAD:-}" ]; then
  gh auth status >/dev/null 2>&1 || { echo "Sign in first: gh auth login"; exit 1; }
fi
if [ -e "$KEY" ]; then
  echo "A key already exists at $KEY. Not replacing it: a new key could not update the installed app."
  echo "(To upload the existing key again, move it away, or set the secrets by hand as README.md describes.)"
  exit 1
fi

read -r -s -p "Choose a password for the key (at least 6 characters): " PASS; echo
read -r -s -p "Type it again: " PASS2; echo
[ "$PASS" = "$PASS2" ] || { echo "The two passwords differ. Nothing was made."; exit 1; }
[ "${#PASS}" -ge 6 ] || { echo "Too short. Nothing was made."; exit 1; }
export PASS

mkdir -p "$DIR"; chmod 700 "$DIR"
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
openssl req -x509 -newkey rsa:2048 -sha256 -days 10000 -nodes -subj "/CN=Interstitial Diary" \
  -keyout "$tmp/key.pem" -out "$tmp/cert.pem" 2>/dev/null
openssl pkcs12 -export -inkey "$tmp/key.pem" -in "$tmp/cert.pem" -name "$ALIAS" -passout env:PASS -out "$KEY"
chmod 600 "$KEY"
echo "Key written to $KEY"

if [ -n "${SKIP_UPLOAD:-}" ]; then echo "SKIP_UPLOAD is set: the secrets were not changed."; exit 0; fi
base64 -w0 "$KEY" | gh secret set ANDROID_KEYSTORE_BASE64
printf '%s' "$PASS" | gh secret set ANDROID_KEYSTORE_PASSWORD
printf '%s' "$ALIAS" | gh secret set ANDROID_KEY_ALIAS
echo
echo "Done. The next build signs the APK with this key."
echo "Now copy $KEY and the password somewhere safe."
