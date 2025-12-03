#!/usr/bin/env bash
set -euo pipefail

HOST_DIR="${1:-}"
ACTION="${2:-up}"
if [[ -z "$HOST_DIR" ]]; then
  echo "Usage: $0 <hosts/prd-compute|hosts/prd-stateful|hosts/dev-infra> [up|down|restart|ps|logs]"
  exit 1
fi

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOCKER_DIR="$BASE_DIR/docker"
HOST_PATH="$DOCKER_DIR/$HOST_DIR"

if [[ ! -d "$HOST_PATH" ]]; then
  echo "Host folder not found: $HOST_PATH"
  exit 1
fi

compose_common=(-f "$DOCKER_DIR/base/networks-volumes.yml")

case "$HOST_DIR" in
  hosts/prd-compute)
    compose_files=(
      -f "$DOCKER_DIR/base/proxy.yml"
      -f "$DOCKER_DIR/base/api.yml"
      -f "$DOCKER_DIR/base/gotenberg.yml"
      -f "$DOCKER_DIR/base/redis.yml"
      -f "$HOST_PATH/compose.yml"
    )
    env_files=(
      --env-file "$DOCKER_DIR/env/common.env"
      --env-file "$DOCKER_DIR/env/api.env"
      --env-file "$DOCKER_DIR/env/gotenberg.env"
      --env-file "$DOCKER_DIR/env/redis.env"
      --env-file "$HOST_PATH/.env"
    )
    ;;
  hosts/prd-stateful)
    compose_files=(
      -f "$DOCKER_DIR/base/postgres.yml"
      -f "$DOCKER_DIR/base/minio.yml"
      -f "$HOST_PATH/compose.yml"
    )
    env_files=(
      --env-file "$DOCKER_DIR/env/common.env"
      --env-file "$DOCKER_DIR/env/postgres.env"
      --env-file "$DOCKER_DIR/env/minio.env"
      --env-file "$HOST_PATH/.env"
    )
    ;;
  hosts/dev-infra)
    compose_files=(
      -f "$DOCKER_DIR/base/gotenberg.yml"
      -f "$DOCKER_DIR/base/redis.yml"
      -f "$DOCKER_DIR/base/postgres.yml"
      -f "$DOCKER_DIR/base/minio.yml"
      -f "$HOST_PATH/compose.yml"
    )
    env_files=(
      --env-file "$DOCKER_DIR/env/common.env"
      --env-file "$DOCKER_DIR/env/gotenberg.env"
      --env-file "$DOCKER_DIR/env/redis.env"
      --env-file "$DOCKER_DIR/env/postgres.env"
      --env-file "$DOCKER_DIR/env/minio.env"
      --env-file "$HOST_PATH/.env"
    )
    ;;
  *)
    echo "Unknown host: $HOST_DIR"
    exit 1
    ;;
esac

cd "$DOCKER_DIR"

case "$ACTION" in
  up) docker compose "${compose_common[@]}" "${compose_files[@]}" "${env_files[@]}" up -d ;;
  down) docker compose "${compose_common[@]}" "${compose_files[@]}" "${env_files[@]}" down ;;
  restart) docker compose "${compose_common[@]}" "${compose_files[@]}" "${env_files[@]}" down &&            docker compose "${compose_common[@]}" "${compose_files[@]}" "${env_files[@]}" up -d ;;
  ps) docker compose "${compose_common[@]}" "${compose_files[@]}" "${env_files[@]}" ps ;;
  logs) docker compose "${compose_common[@]}" "${compose_files[@]}" "${env_files[@]}" logs -f ;;
  *) echo "Unknown action: $ACTION"; exit 1 ;;
esac
