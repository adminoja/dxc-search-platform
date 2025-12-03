# Dev Infra Stack (no API)
Starts Redis, Postgres, MinIO, and Gotenberg locally.

## Start
docker compose   -f ../../base/networks-volumes.yml   -f ../../base/gotenberg.yml   -f ../../base/redis.yml   -f ../../base/postgres.yml   -f ../../base/minio.yml   -f ./compose.yml   --env-file ../../env/common.env   --env-file ../../env/gotenberg.env   --env-file ../../env/redis.env   --env-file ../../env/postgres.env   --env-file ../../env/minio.env   --env-file ./.env   up -d
