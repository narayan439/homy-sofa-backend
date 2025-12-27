## Multi-stage Dockerfile for building and running the Spring Boot backend
# - Build stage uses an official Maven + JDK image to produce the runnable jar
# - Final stage runs the jar on a lightweight JRE image

### Usage
# Build image (from repository root or inside backend/Homy-backend):
# docker build -t homy-backend:latest .
# Run:
# docker run -p 8080:8080 --name homy-backend homy-backend:latest

FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy pom and sources
COPY pom.xml .
COPY mvnw mvnw
COPY .mvn .mvn
COPY src src

# Use the Maven wrapper if present, otherwise use the image's mvn
RUN if [ -x mvnw ]; then ./mvnw -B -DskipTests package; else mvn -B -DskipTests package; fi

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the jar produced by the build stage
COPY --from=build /workspace/target/*.jar ./app.jar

# Expose default Spring Boot port
EXPOSE 8080

ENV JAVA_OPTS="-Xms128m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

