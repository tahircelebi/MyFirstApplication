#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
PID_FILE="$PROJECT_DIR/.camel_server.pid"
LOG_FILE="$PROJECT_DIR/camel_server.log"

cd "$PROJECT_DIR"

if [[ -f "$PID_FILE" ]]; then
  OLD_PID="$(cat "$PID_FILE")"
  if kill -0 "$OLD_PID" 2>/dev/null; then
    echo "Server already running (PID $OLD_PID)"
    echo "URL: http://localhost:8080/login"
    exit 0
  else
    rm -f "$PID_FILE"
  fi
fi

nohup mvn -q exec:java > "$LOG_FILE" 2>&1 < /dev/null &
NEW_PID=$!
echo "$NEW_PID" > "$PID_FILE"

sleep 4
if curl -fsS http://localhost:8080/login >/dev/null 2>&1; then
  echo "Server started (PID $NEW_PID)"
  echo "URL: http://localhost:8080/login"
  exit 0
fi

echo "Server did not start correctly. Check log: $LOG_FILE"
exit 1
