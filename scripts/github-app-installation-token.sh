#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage: github-app-installation-token.sh [--raw-token] [--skip-verify]

Required environment variables:
  GITHUB_APP_ID                   GitHub App ID
  GITHUB_APP_INSTALLATION_ID      GitHub App installation ID
  GITHUB_APP_PRIVATE_KEY_FILE     Path to the GitHub App PEM private key

Optional environment variables:
  GITHUB_REPOSITORY               owner/repo to verify against (default: bmj16060/constructraos)
  GITHUB_API_URL                  GitHub API base URL (default: https://api.github.com)
EOF
}

RAW_TOKEN=false
SKIP_VERIFY=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --raw-token)
      RAW_TOKEN=true
      shift
      ;;
    --skip-verify)
      SKIP_VERIFY=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

: "${GITHUB_APP_ID:?Set GITHUB_APP_ID.}"
: "${GITHUB_APP_INSTALLATION_ID:?Set GITHUB_APP_INSTALLATION_ID.}"
: "${GITHUB_APP_PRIVATE_KEY_FILE:?Set GITHUB_APP_PRIVATE_KEY_FILE.}"

if [[ ! -f "$GITHUB_APP_PRIVATE_KEY_FILE" ]]; then
  echo "Private key file not found: $GITHUB_APP_PRIVATE_KEY_FILE" >&2
  exit 1
fi

GITHUB_REPOSITORY="${GITHUB_REPOSITORY:-bmj16060/constructraos}"
GITHUB_API_URL="${GITHUB_API_URL:-https://api.github.com}"

b64url() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

now_epoch="$(date +%s)"
issued_at="$((now_epoch - 60))"
expires_at="$((now_epoch + 540))"

jwt_header='{"alg":"RS256","typ":"JWT"}'
jwt_payload="$(printf '{"iat":%s,"exp":%s,"iss":"%s"}' "$issued_at" "$expires_at" "$GITHUB_APP_ID")"
jwt_unsigned="$(printf '%s' "$jwt_header" | b64url).$(printf '%s' "$jwt_payload" | b64url)"
jwt_signature="$(
  printf '%s' "$jwt_unsigned" \
    | openssl dgst -binary -sha256 -sign "$GITHUB_APP_PRIVATE_KEY_FILE" \
    | openssl base64 -A \
    | tr '+/' '-_' \
    | tr -d '='
)"
jwt="${jwt_unsigned}.${jwt_signature}"

token_response="$(
  curl --silent --show-error --fail \
    -X POST \
    -H "Accept: application/vnd.github+json" \
    -H "Authorization: Bearer ${jwt}" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    "${GITHUB_API_URL}/app/installations/${GITHUB_APP_INSTALLATION_ID}/access_tokens"
)"

installation_token="$(printf '%s' "$token_response" | jq -r '.token')"
expires_at_iso="$(printf '%s' "$token_response" | jq -r '.expires_at')"

if [[ -z "$installation_token" || "$installation_token" == "null" ]]; then
  echo "GitHub did not return an installation token." >&2
  printf '%s\n' "$token_response" >&2
  exit 1
fi

if [[ "$SKIP_VERIFY" == "false" ]]; then
  verification_response="$(
    curl --silent --show-error --fail \
      -H "Accept: application/vnd.github+json" \
      -H "Authorization: Bearer ${installation_token}" \
      -H "X-GitHub-Api-Version: 2022-11-28" \
      "${GITHUB_API_URL}/repos/${GITHUB_REPOSITORY}"
  )"
  verified_full_name="$(printf '%s' "$verification_response" | jq -r '.full_name')"
  if [[ "$verified_full_name" != "$GITHUB_REPOSITORY" ]]; then
    echo "Repository verification failed for ${GITHUB_REPOSITORY}." >&2
    printf '%s\n' "$verification_response" >&2
    exit 1
  fi
fi

if [[ "$RAW_TOKEN" == "true" ]]; then
  printf '%s\n' "$installation_token"
  exit 0
fi

cat <<EOF
GitHub App installation token minted successfully.
App ID: ${GITHUB_APP_ID}
Installation ID: ${GITHUB_APP_INSTALLATION_ID}
Repository: ${GITHUB_REPOSITORY}
Token expires at: ${expires_at_iso}
Verification: $( [[ "$SKIP_VERIFY" == "true" ]] && printf 'skipped' || printf 'passed' )
EOF
