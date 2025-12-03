@echo off
setlocal

REM Change to the folder where this script lives
cd /d "%~dp0"

REM Env file for this host
set ENV_FILE=hosts\dev-infra\.env

REM Common compose files for dev infra (one per line, using ^ as continuation)
set COMPOSE_FILES=-f base\networks-volumes.yml ^
 -f base\postgres.yml ^
 -f base\gotenberg.yml ^
 -f base\wiremock.yml

REM Forward all arguments (%*) to docker compose
docker compose --env-file "%ENV_FILE%" %COMPOSE_FILES% %*

endlocal
