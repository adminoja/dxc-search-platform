@echo off
setlocal enabledelayedexpansion

if "%~1"=="" (
  echo Usage: %~nx0 ^<hosts/prd-compute^|hosts/prd-stateful^|hosts/dev-infra^> [up^|down^|restart^|ps^|logs]
  exit /b 1
)

set HOST_DIR=%1
set ACTION=%2
if "%ACTION%"=="" set ACTION=up

for %%I in ("%~dp0..") do set BASE_DIR=%%~fI
set DOCKER_DIR=%BASE_DIR%\docker
set HOST_PATH=%DOCKER_DIR%\%HOST_DIR%

if not exist "%HOST_PATH%" (
  echo Host folder not found: %HOST_PATH%
  exit /b 1
)

set COMPOSE_COMMON=-f "%DOCKER_DIR%\base\networks-volumes.yml"

if "%HOST_DIR%"=="hosts/prd-compute" (
  set COMPOSE_FILES=-f "%DOCKER_DIR%\base\proxy.yml" -f "%DOCKER_DIR%\base\api.yml" -f "%DOCKER_DIR%\base\gotenberg.yml" -f "%DOCKER_DIR%\base\redis.yml" -f "%HOST_PATH%\compose.yml"
  set ENV_FILES=--env-file "%DOCKER_DIR%\env\common.env" --env-file "%DOCKER_DIR%\env\api.env" --env-file "%DOCKER_DIR%\env\gotenberg.env" --env-file "%DOCKER_DIR%\env\redis.env" --env-file "%HOST_PATH%\.env"
) else if "%HOST_DIR%"=="hosts/prd-stateful" (
  set COMPOSE_FILES=-f "%DOCKER_DIR%\base\postgres.yml" -f "%DOCKER_DIR%\base\minio.yml" -f "%HOST_PATH%\compose.yml"
  set ENV_FILES=--env-file "%DOCKER_DIR%\env\common.env" --env-file "%DOCKER_DIR%\env\postgres.env" --env-file "%DOCKER_DIR%\env\minio.env" --env-file "%HOST_PATH%\.env"
) else if "%HOST_DIR%"=="hosts/dev-infra" (
  set COMPOSE_FILES=-f "%DOCKER_DIR%\base\gotenberg.yml" -f "%DOCKER_DIR%\base\redis.yml" -f "%DOCKER_DIR%\base\postgres.yml" -f "%DOCKER_DIR%\base\minio.yml" -f "%HOST_PATH%\compose.yml"
  set ENV_FILES=--env-file "%DOCKER_DIR%\env\common.env" --env-file "%DOCKER_DIR%\env\gotenberg.env" --env-file "%DOCKER_DIR%\env\redis.env" --env-file "%DOCKER_DIR%\env\postgres.env" --env-file "%DOCKER_DIR%\env\minio.env" --env-file "%HOST_PATH%\.env"
) else (
  echo Unknown host: %HOST_DIR%
  exit /b 1
)

pushd "%DOCKER_DIR%"
if "%ACTION%"=="up" (
  docker compose %COMPOSE_COMMON% %COMPOSE_FILES% %ENV_FILES% up -d
) else if "%ACTION%"=="down" (
  docker compose %COMPOSE_COMMON% %COMPOSE_FILES% %ENV_FILES% down
) else if "%ACTION%"=="restart" (
  docker compose %COMPOSE_COMMON% %COMPOSE_FILES% %ENV_FILES% down
  docker compose %COMPOSE_COMMON% %COMPOSE_FILES% %ENV_FILES% up -d
) else if "%ACTION%"=="ps" (
  docker compose %COMPOSE_COMMON% %COMPOSE_FILES% %ENV_FILES% ps
) else if "%ACTION%"=="logs" (
  docker compose %COMPOSE_COMMON% %COMPOSE_FILES% %ENV_FILES% logs -f
) else (
  echo Unknown action: %ACTION%
  popd
  exit /b 1
)
popd
