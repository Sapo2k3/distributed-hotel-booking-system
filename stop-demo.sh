#!/usr/bin/env bash
set -e

echo "Stopping Docker Compose services..."
docker compose down

echo "Services stopped."
echo "Note: Docker volumes are preserved."