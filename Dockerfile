# Multi-stage build for Free Roulette Spin System
FROM maven:3.9-openjdk-17-slim AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM openjdk:17-jdk-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN groupadd -r casino && useradd -r -g casino casino

# Copy built jar from builder stage
COPY --from=builder /app/target/free-roulette-spin-system-*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && chown -R casino:casino /app

# Switch to non-root user
USER casino

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# JVM options for production
ENV JVM_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/"

# Run application
ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar app.jar"]