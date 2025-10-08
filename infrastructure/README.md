# infrastructure/
This folder contains Docker Compose definitions for running **dxc-search-platform** and its local dependencies (MinIO, Gotenberg, PostgreSQL).

## Folders
- `compose/base/` — shared service definitions (no secrets). Always included.
- `compose/local/` — local developer overrides (ports, bind mounts). Use with `.env.local`.
- `compose/prod/` — production overrides. Use with `.env.prod` supplied by CI or ops.

> Keep real secrets out of Git. Use env files that are **gitignored** or use your CI/CD secret store.
