# infrastructure/compose/local/
Local developer overrides.

## How to run locally
1) Create your env file:
   ```bash
   cp infrastructure/compose/base/.env.example infrastructure/compose/local/.env.local
   ```
2) Start:
   ```bash
   docker compose          -f infrastructure/compose/base/docker-compose.yml          -f infrastructure/compose/local/docker-compose.local.yml          --env-file infrastructure/compose/local/.env.local          up -d
   ```
3) Useful URLs:
   - MinIO Console: http://localhost:9001 (login with MINIO_ROOT_USER/PASSWORD)
   - Gotenberg Health: http://localhost:3000/health
   - Postgres: localhost:5432
   - dxc-search-platform: http://localhost:8081

## Notes
- `certs/` is for **dev-only** keys (e.g., demo PKCS#12). Keep it out of Git.
- The application should set `SPRING_PROFILES_ACTIVE=local`.
