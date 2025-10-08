# infrastructure/compose/
Compose files are split into **base** + **environment-specific overrides**.

Typical commands (run from your repo root):
```bash
# Local (first time)
cp infrastructure/compose/base/.env.example infrastructure/compose/local/.env.local
# edit .env.local if needed

# Up locally
docker compose       -f infrastructure/compose/base/docker-compose.yml       -f infrastructure/compose/local/docker-compose.local.yml       --env-file infrastructure/compose/local/.env.local       up -d

# Down locally
docker compose       -f infrastructure/compose/base/docker-compose.yml       -f infrastructure/compose/local/docker-compose.local.yml       --env-file infrastructure/compose/local/.env.local       down -v
```

For production, provide a secure `.env.prod` (not committed) and use the `prod` override file.
