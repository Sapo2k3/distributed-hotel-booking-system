#!/usr/bin/env bash
set -e

echo "Building project jars..."
mvn clean package -DskipTests

echo "Starting Docker Compose services..."
docker compose up --build -d

echo ""
echo "Services status:"
docker compose ps

echo ""
echo "Coordinator endpoint:"
echo "  http://localhost:8080/hotels"
echo ""
echo "Hotel nodes:"
echo "  http://localhost:8081/health"
echo "  http://localhost:8082/health"
echo ""
echo "You can now start the JavaFX GUI locally."