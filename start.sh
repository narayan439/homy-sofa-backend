#!/usr/bin/env sh
set -e

echo "Starting Spring Boot application..."

# Go to backend root
cd "$(dirname "$0")"

# If jar already exists, run it
JAR=$(ls target/*.jar 2>/dev/null | head -n 1 || true)
if [ -n "$JAR" ]; then
  echo "Found existing jar: $JAR"
  exec java -jar "$JAR"
fi

# Build using Maven Wrapper
if [ -x mvnw ]; then
  echo "Building with Maven Wrapper"
  ./mvnw clean package -DskipTests
else
  echo "mvnw not found, using system mvn"
  mvn clean package -DskipTests
fi

# Run the built jar
JAR=$(ls target/*.jar | head -n 1)
echo "Running jar: $JAR"
exec java -jar "$JAR"
