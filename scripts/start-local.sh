#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd "$(dirname "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd "$SCRIPT_DIR/.." && pwd)

cd "$ROOT_DIR"

if [ ! -d "frontend/node_modules" ]; then
  echo "frontend/node_modules nao encontrado. Execute primeiro: cd frontend && npm install"
  exit 1
fi

docker compose up -d

mvn spring-boot:run &
BACKEND_PID=$!

(
  cd frontend
  npm run dev
) &
FRONTEND_PID=$!

cleanup() {
  kill "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null || true
}

trap cleanup INT TERM EXIT

echo "Backend: http://localhost:8080"
echo "Dashboard: veja a URL Local impressa pelo Vite"

wait
