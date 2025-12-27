#!/usr/bin/env sh
# Simple start script for Railpack / buildpacks to detect and run the app.
# Behavior:
# - If a built Spring Boot jar exists under backend/Homy-backend/target, run it.
# - Else attempt to build and run the backend with the Maven wrapper if available.
# - If no backend found, fallback to starting the frontend with `npm start` if package.json exists.

set -e

ROOT_DIR=$(pwd)

echo "Starting app via start.sh (detected root: $ROOT_DIR)"

if [ -d "backend/Homy-backend" ]; then
  cd backend/Homy-backend

  # If a packaged jar exists, run it directly
  JAR=$(ls target/*.jar 2>/dev/null | head -n 1 || true)
  if [ -n "$JAR" ]; then
    echo "Found jar: $JAR — running"
    exec java -jar "$JAR"
  fi

  # If Maven wrapper exists, use it to build & run; otherwise try mvn
  if [ -x mvnw ]; then
    echo "Building with Maven wrapper (mvnw) and starting Spring Boot"
    ./mvnw -B -DskipTests package
    JAR=$(ls target/*.jar | head -n 1)
    exec java -jar "$JAR"
  else
    echo "No mvnw found — attempting to use system mvn"
    mvn -B -DskipTests package
    JAR=$(ls target/*.jar | head -n 1)
    exec java -jar "$JAR"
  fi

else
  # No backend present — try frontend
  if [ -f "package.json" ]; then
    echo "No backend found; starting frontend dev server via npm"
    npm install --no-audit --no-fund
    exec npm start
  fi

  echo "No backend or frontend start command found. Exiting."
  exit 1
fi
