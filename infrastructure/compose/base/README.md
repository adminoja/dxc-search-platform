# infrastructure/compose/base/
**Base** Compose file and a documented `.env.example`. The base file only contains reusable,
environment-agnostic service definitions. Do **not** put secrets here.

Include this file in _every_ `docker compose` command, then layer an environment override:
- Local: `compose/local/docker-compose.local.yml`
- Prod : `compose/prod/docker-compose.prod.yml`
