#!/usr/bin/env bash
set -euo pipefail

ROOT="/srv/dxc-search-platform"
COMPOSE_FILE="$ROOT/docker-compose.yml"
DOT_ENV="$ROOT/.env"

PURGE=0        # also remove named volumes
NUKE=0         # remove everything under /srv/dxc-search-platform (after stopping)
IMAGES=0       # remove images used by this compose project

usage() {
  cat <<EOF
Usage: $(basename "$0") [--purge] [--images] [--nuke]

Stops and removes the dxc-search-platform compose stack.

Options:
  --purge   Remove named volumes (data loss for volumes: app_logs, gotenberg_tmp)
  --images  Remove images used by this project (after down)
  --nuke    Delete ALL files under $ROOT (certs, env, logs, volumes, compose, bin)

Examples:
  $(basename "$0")            # stop containers, keep volumes, keep files
  $(basename "$0") --purge    # stop + remove volumes
  $(basename "$0") --purge --images
  $(basename "$0") --nuke     # stop + delete $ROOT (asks for confirmation)
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --purge) PURGE=1; shift ;;
    --images) IMAGES=1; shift ;;
    --nuke) NUKE=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 2 ;;
  endesac
done

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Compose file not found: $COMPOSE_FILE" >&2
  exit 1
fi

cd "$ROOT"

echo "[uninstall] Bringing stack down..."
if [[ $PURGE -eq 1 ]]; then
  docker compose down -v || true
else
  docker compose down || true
fi

if [[ $IMAGES -eq 1 ]]; then
  echo "[uninstall] Removing project images..."
  # remove images referenced by this compose project (best-effort)
  docker compose images --quiet | xargs -r docker rmi -f || true
fi

echo "[uninstall] Pruning dangling resources (safe)..."
docker image prune -f || true
docker volume prune -f || true

if [[ $NUKE -eq 1 ]]; then
  read -r -p "⚠️  This will DELETE EVERYTHING under $ROOT. Type 'DELETE' to confirm: " CONF
  if [[ "$CONF" == "DELETE" ]]; then
    echo "[uninstall] Deleting $ROOT ..."
    rm -rf "$ROOT"
    echo "[uninstall] Done. Directory removed."
  else
    echo "[uninstall] Aborted. $ROOT was NOT deleted."
  fi
else
  echo "[uninstall] Done. Files remain under $ROOT."
fi
