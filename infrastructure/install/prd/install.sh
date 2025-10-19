#!/usr/bin/env bash
set -euo pipefail

# === Settings ===
APP_ROOT="/srv/dxc-search-platform"

# Source filenames expected in the *current* directory (override via env if needed)
COMPOSE_FILE_SRC="${COMPOSE_FILE_SRC:-docker-compose.yml}"
ENV_FILE_SRC="${ENV_FILE_SRC:-prd.env}"
DEPLOY_FILE_SRC="${DEPLOY_FILE_SRC:-deploy.sh}"

DO_DEPLOY=0
DO_BACKUP=0

usage() {
  cat <<EOF
Usage: $(basename "$0") [--deploy] [--backup]

Fully replaces install targets under: $APP_ROOT

Copies (overwriting existing):
  ./$COMPOSE_FILE_SRC  -> $APP_ROOT/docker-compose.yml
  ./$ENV_FILE_SRC      -> $APP_ROOT/env/prd.env  (and updates $APP_ROOT/.env symlink)
  ./$DEPLOY_FILE_SRC   -> $APP_ROOT/bin/deploy.sh (0755)

Also copies certs from current dir (overwriting):
  *.crt *.cer *.pem -> $APP_ROOT/certs/ (0644)
  *.pfx *.p12       -> $APP_ROOT/certs/ (0600)

Options:
  --deploy   Run initial deployment after install (calls bin/deploy.sh prd)
  --backup   Before overwriting any target, save a .bak-YYYYmmdd_HHMMSS copy
  -h, --help Show this help
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --deploy) DO_DEPLOY=1; shift ;;
    --backup) DO_BACKUP=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 2 ;;
  esac
done

# --- Validate sources ---
for f in "$COMPOSE_FILE_SRC" "$ENV_FILE_SRC" "$DEPLOY_FILE_SRC"; do
  if [[ ! -f "$f" ]]; then
    echo "ERROR: Source file not found in current directory: $f" >&2
    exit 1
  fi
done

# --- Create directories ---
echo "[install] Ensuring directory structure under $APP_ROOT ..."
sudo mkdir -p \
  "$APP_ROOT/env" \
  "$APP_ROOT/certs" \
  "$APP_ROOT/logs" \
  "$APP_ROOT/volumes" \
  "$APP_ROOT/bin"

backup_if_needed() {
  local target="$1"
  if [[ "$DO_BACKUP" -eq 1 && -e "$target" ]]; then
    local ts; ts="$(date +%Y%m%d_%H%M%S)"
    local bak="${target}.bak-${ts}"
    echo "[install]   - Backing up: $target -> $bak"
    sudo cp -a "$target" "$bak"
  fi
}

# --- Install main files (overwrite) ---
install_file() {
  local src="$1" dst="$2" mode="$3"
  backup_if_needed "$dst"
  echo "[install] Installing: $src -> $dst (mode $mode)"
  sudo install -m "$mode" "$src" "$dst"
}

install_file "$COMPOSE_FILE_SRC" "$APP_ROOT/docker-compose.yml" 0644
install_file "$ENV_FILE_SRC"     "$APP_ROOT/env/prd.env"        0644
install_file "$DEPLOY_FILE_SRC"  "$APP_ROOT/bin/deploy.sh"      0755

# --- Ensure .env symlink to env/prd.env (overwrite if exists) ---
DOT_ENV="$APP_ROOT/.env"
if [[ -L "$DOT_ENV" || -f "$DOT_ENV" ]]; then
  backup_if_needed "$DOT_ENV"
  sudo rm -f "$DOT_ENV"
fi
echo "[install] Linking .env -> env/prd.env"
( cd "$APP_ROOT" && sudo ln -s "env/prd.env" ".env" )

# --- Copy certs from current dir (overwrite) ---
shopt -s nullglob nocaseglob
CERT_DEST="$APP_ROOT/certs"
PUB_CERTS=( *.crt *.cer *.pem )
PFX_CERTS=( *.pfx *.p12 )

if (( ${#PUB_CERTS[@]} )); then
  echo "[install] Copying public certs to $CERT_DEST ..."
  for c in "${PUB_CERTS[@]}"; do
    dst="$CERT_DEST/$(basename "$c")"
    backup_if_needed "$dst"
    sudo install -m 0644 "$c" "$dst"
    echo "[install]   - Installed: $dst (0644)"
  done
fi

if (( ${#PFX_CERTS[@]} )); then
  echo "[install] Copying PKCS#12 bundles to $CERT_DEST ..."
  for c in "${PFX_CERTS[@]}"; do
    dst="$CERT_DEST/$(basename "$c")"
    backup_if_needed "$dst"
    sudo install -m 0600 "$c" "$dst"
    echo "[install]   - Installed: $dst (0600)"
  done
fi
shopt -u nullglob nocaseglob

# --- Ownership to current user for convenience ---
echo "[install] Setting ownership to $USER:$USER ..."
sudo chown -R "$USER:$USER" "$APP_ROOT"

# --- Sanity checks ---
if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker not found in PATH. Install Docker Engine first." >&2
  exit 1
fi
if ! docker compose version >/dev/null 2>&1; then
  echo "ERROR: 'docker compose' plugin not available. Install Docker Compose v2." >&2
  exit 1
fi

# --- Optional initial deploy ---
if [[ "$DO_DEPLOY" -eq 1 ]]; then
  echo "[install] Running initial deployment ..."
  "$APP_ROOT/bin/deploy.sh" prd
else
  cat <<EOF

[install] Done.

You can now:
  cd $APP_ROOT
  docker compose up -d     # uses docker-compose.yml + .env -> env/prd.env

Or via helper:
  $APP_ROOT/bin/deploy.sh prd

Tip: re-run with --backup to keep .bak copies of replaced files.
EOF
fi
