# Multi-stage Dockerfile for EasyLearn backend
# Stage 1: build the fat jar using Maven
FROM maven:3.8.4-openjdk-17 AS builder
WORKDIR /workspace

# Copy Maven wrapper & pom to leverage layer caching for dependencies
COPY pom.xml ./


# Pre-download dependencies
RUN mvn -B -f pom.xml -DskipTests dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -B -f pom.xml -DskipTests package

# Stage 2: runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the jar produced in the builder stage
COPY --from=builder /workspace/target/*.jar /app/app.jar

# Expose the application port (default defined in application.yml)
EXPOSE 8081

# Allow overriding JAVA options and Spring profile at runtime.
# By default the application will run with the 'prod' profile inside the container; you can override SPRING_PROFILES_ACTIVE when running.
ENV SPRING_PROFILES_ACTIVE=dev

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar /app/app.jar"]
