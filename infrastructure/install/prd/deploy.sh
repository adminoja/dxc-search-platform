#!/usr/bin/env bash
set -euo pipefail

APP_ROOT="/srv/dxc-search-platform"
ENV_NAME="${1:-prd}"           # prd|stg|sit|dev (optional; defaults to prd)
APP_TAG_OVERRIDE="${2:-}"      # optional image tag override

COMPOSE_DIR="$APP_ROOT/compose/base"
ENV_FILE="$APP_ROOT/env/${ENV_NAME}.env"

if [[ ! -f "$COMPOSE_DIR/docker-compose.yml" ]]; then
  echo "Compose file not found: $COMPOSE_DIR/docker-compose.yml" >&2
  exit 1
fi
if [[ ! -f "$ENV_FILE" ]]; then
  echo "Env file not found: $ENV_FILE" >&2
  exit 1
fi

# Optional tag override at runtime
if [[ -n "$APP_TAG_OVERRIDE" ]]; then
  export APP_TAG="$APP_TAG_OVERRIDE"
  echo "[deploy] Overriding APP_TAG -> $APP_TAG"
fi

echo "[deploy] Using compose at: $COMPOSE_DIR"
echo "[deploy] Using env file  : $ENV_FILE"

cd "$COMPOSE_DIR"

# Pull & run
docker compose --env-file "$ENV_FILE" pull
docker compose --env-file "$ENV_FILE" up -d

# Clean unused images to save disk
docker image prune -f

echo "[deploy] Done."
