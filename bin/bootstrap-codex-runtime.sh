#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE_HOME="${CODEX_BOOTSTRAP_SOURCE_HOME:-$HOME/.codex}"
TARGET_DIR="${CODEX_BOOTSTRAP_TARGET_DIR:-$ROOT_DIR/.codex-runtime}"

usage() {
  cat <<'EOF'
Usage: ./bin/bootstrap-codex-runtime.sh [--force]

Copies local Codex auth/config from ~/.codex into repo-local .codex-runtime/
so docker compose can configure the codex-runtime container explicitly.

Options:
  --force   overwrite existing target files
EOF
}

force=0
if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi
if [[ "${1:-}" == "--force" ]]; then
  force=1
elif [[ $# -gt 0 ]]; then
  usage >&2
  exit 1
fi

auth_source="$SOURCE_HOME/auth.json"
config_source="$SOURCE_HOME/config.toml"
auth_target="$TARGET_DIR/auth.json"
config_target="$TARGET_DIR/config.toml"

if [[ ! -f "$auth_source" ]]; then
  echo "Missing source auth file: $auth_source" >&2
  exit 1
fi

mkdir -p "$TARGET_DIR"
chmod 700 "$TARGET_DIR"

copy_file() {
  local source="$1"
  local target="$2"
  local required="$3"

  if [[ ! -f "$source" ]]; then
    if [[ "$required" == "required" ]]; then
      echo "Missing required source file: $source" >&2
      exit 1
    fi
    return
  fi

  if [[ -f "$target" && "$force" -ne 1 ]]; then
    echo "Target already exists: $target (use --force to overwrite)" >&2
    exit 1
  fi

  cp "$source" "$target"
  chmod 600 "$target"
}

copy_file "$auth_source" "$auth_target" required
copy_file "$config_source" "$config_target" optional

echo "Seeded $TARGET_DIR from $SOURCE_HOME"
