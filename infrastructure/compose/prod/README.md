# infrastructure/compose/prod/
Production overrides. Prefer pulling a prebuilt image and passing secrets via CI/CD.

## Deploy (example)
```bash
docker compose       -f infrastructure/compose/base/docker-compose.yml       -f infrastructure/compose/prod/docker-compose.prod.yml       --env-file infrastructure/compose/prod/.env.prod       up -d
```

## Guidance
- Do not use bind mounts in prod.
- Pin images and enable restart policies + log rotation.
- Keep `.env.prod` outside Git; generate it from your secrets store (Vault/KMS/CI).
- Consider externalizing stateful services (managed Postgres/object storage) where possible.
