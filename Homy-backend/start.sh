#!/bin/sh
echo "Starting Homy Backend..."

./mvnw clean package -DskipTests
java -jar target/*SNAPSHOT.jar
