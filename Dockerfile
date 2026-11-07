# ===================================================================
# Multi-Stage Dockerfile for Spring Boot (Poker Settlement Backend)
# ===================================================================

# Stage 1: Build the JAR
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

# Copy Maven wrapper and POM first for efficient layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy application source code and compile
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create a dedicated non-root user for security
RUN groupadd -r pokergroup && useradd -r -g pokergroup -s /bin/false pokeruser

# Install curl for container health check
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Copy packaged JAR from build stage
COPY --from=builder /workspace/target/poker-settlement-*.jar /app/app.jar
RUN chown -R pokeruser:pokergroup /app

USER pokeruser

# Standard Spring Boot port
EXPOSE 8080

# Environment defaults
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

# Healthcheck using Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
