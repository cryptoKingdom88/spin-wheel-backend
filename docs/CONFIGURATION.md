# Free Roulette Spin System - Configuration Guide

## Overview

This document describes all configuration options and environment variables for the Free Roulette Spin System. The application uses Spring Boot's configuration system with support for environment variables, property files, and profiles.

## Table of Contents

1. [Environment Variables](#environment-variables)
2. [Application Properties](#application-properties)
3. [Database Configuration](#database-configuration)
4. [Logging Configuration](#logging-configuration)
5. [Profile-Specific Configuration](#profile-specific-configuration)
6. [Docker Configuration](#docker-configuration)
7. [Production Deployment](#production-deployment)

## Environment Variables

### Required Environment Variables

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `DB_USERNAME` | Database username | `casino_user` | `roulette_prod_user` |
| `DB_PASSWORD` | Database password | `casino_password` | `secure_password_123` |
| `DB_HOST` | Database host | `localhost` | `db.casino.com` |
| `DB_PORT` | Database port | `5432` | `5432` |
| `DB_NAME` | Database name | `casino_roulette` | `roulette_production` |

### Optional Environment Variables

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `SERVER_PORT` | Application server port | `8080` | `8080` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profiles | `default` | `production,monitoring` |
| `LOG_LEVEL_ROOT` | Root logging level | `INFO` | `WARN` |
| `LOG_LEVEL_CASINO` | Application logging level | `INFO` | `DEBUG` |
| `JVM_OPTS` | JVM options | - | `-Xmx2g -Xms1g` |

### Security Environment Variables

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `ENCRYPTION_KEY` | Data encryption key | - | `base64encodedkey123` |
| `JWT_SECRET` | JWT signing secret | - | `jwt_secret_key_here` |
| `API_RATE_LIMIT` | API rate limit per minute | `60` | `100` |

## Application Properties

### Core Application Configuration

```yaml
spring:
  application:
    name: free-roulette-spin-system
  
  # Server Configuration
  server:
    port: ${SERVER_PORT:8080}
    servlet:
      context-path: /api
    compression:
      enabled: true
    http2:
      enabled: true
```

### Database Configuration

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:casino_roulette}
    username: ${DB_USERNAME:casino_user}
    password: ${DB_PASSWORD:casino_password}
    driver-class-name: org.postgresql.Driver
    
    # Connection Pool Configuration
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
      max-lifetime: 1200000
      leak-detection-threshold: 60000
  
  # JPA Configuration
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: ${SHOW_SQL:false}
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
    database-platform: org.hibernate.dialect.PostgreSQLDialect
  
  # Flyway Migration Configuration
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
```

### Caching Configuration

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10 minutes
      cache-null-values: false
  
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    timeout: 2000ms
    jedis:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

## Database Configuration

### PostgreSQL Setup

**Development Database:**
```sql
-- Create database
CREATE DATABASE casino_roulette;

-- Create user
CREATE USER casino_user WITH PASSWORD 'casino_password';

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE casino_roulette TO casino_user;
GRANT ALL ON SCHEMA public TO casino_user;
```

**Production Database:**
```sql
-- Create database with specific settings
CREATE DATABASE roulette_production
  WITH ENCODING 'UTF8'
       LC_COLLATE = 'en_US.UTF-8'
       LC_CTYPE = 'en_US.UTF-8'
       TEMPLATE template0;

-- Create user with limited permissions
CREATE USER roulette_prod_user WITH PASSWORD 'secure_password_123';

-- Grant specific permissions
GRANT CONNECT ON DATABASE roulette_production TO roulette_prod_user;
GRANT USAGE ON SCHEMA public TO roulette_prod_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO roulette_prod_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO roulette_prod_user;
```

### Connection Pool Configuration

```yaml
spring:
  datasource:
    hikari:
      # Pool sizing
      maximum-pool-size: ${DB_POOL_MAX_SIZE:20}
      minimum-idle: ${DB_POOL_MIN_IDLE:5}
      
      # Timeouts
      connection-timeout: ${DB_CONNECTION_TIMEOUT:20000}
      idle-timeout: ${DB_IDLE_TIMEOUT:300000}
      max-lifetime: ${DB_MAX_LIFETIME:1200000}
      
      # Health checks
      leak-detection-threshold: ${DB_LEAK_DETECTION:60000}
      validation-timeout: 5000
      
      # Connection properties
      connection-test-query: SELECT 1
      auto-commit: false
```

## Logging Configuration

### Log Levels

```yaml
logging:
  level:
    root: ${LOG_LEVEL_ROOT:INFO}
    com.casino: ${LOG_LEVEL_CASINO:INFO}
    org.springframework.web: ${LOG_LEVEL_WEB:INFO}
    org.hibernate.SQL: ${LOG_LEVEL_SQL:INFO}
    org.hibernate.type.descriptor.sql.BasicBinder: ${LOG_LEVEL_SQL_PARAMS:INFO}
    org.springframework.security: ${LOG_LEVEL_SECURITY:INFO}
```

### Log Output Configuration

```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  
  file:
    name: ${LOG_FILE:logs/roulette-system.log}
    max-size: ${LOG_MAX_SIZE:100MB}
    max-history: ${LOG_MAX_HISTORY:30}
    total-size-cap: ${LOG_TOTAL_SIZE:1GB}
```

### Structured Logging (Production)

```yaml
logging:
  config: classpath:logback-spring.xml
  
# logback-spring.xml
<configuration>
  <springProfile name="production">
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
      <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
        <providers>
          <timestamp/>
          <logLevel/>
          <loggerName/>
          <message/>
          <mdc/>
          <stackTrace/>
        </providers>
      </encoder>
    </appender>
  </springProfile>
</configuration>
```

## Profile-Specific Configuration

### Development Profile (`application-dev.yml`)

```yaml
spring:
  config:
    activate:
      on-profile: dev
  
  datasource:
    url: jdbc:postgresql://localhost:5432/casino_roulette_dev
  
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update
  
  flyway:
    clean-disabled: false

logging:
  level:
    com.casino: DEBUG
    org.hibernate.SQL: DEBUG

# Development-specific settings
management:
  endpoints:
    web:
      exposure:
        include: "*"
```

### Testing Profile (`application-test.yml`)

```yaml
spring:
  config:
    activate:
      on-profile: test
  
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
  
  flyway:
    enabled: false

logging:
  level:
    com.casino: DEBUG
```

### Production Profile (`application-prod.yml`)

```yaml
spring:
  config:
    activate:
      on-profile: production
  
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
  
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate

logging:
  level:
    root: WARN
    com.casino: INFO

# Production security settings
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: never
```

## Docker Configuration

For complete deployment instructions, see [DEPLOYMENT.md](DEPLOYMENT.md).

### Quick Deployment

**Development Environment:**
```bash
docker-compose up -d
```

**Production Environment:**
```bash
cp .env.example .env.prod
# Edit .env.prod with production values
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### Dockerfile

```dockerfile
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy application jar
COPY target/free-roulette-spin-system-1.0.0.jar app.jar

# Create non-root user
RUN groupadd -r casino && useradd -r -g casino casino
RUN chown -R casino:casino /app
USER casino

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# Expose port
EXPOSE 8080

# JVM options
ENV JVM_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC"

# Run application
ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar app.jar"]
```

### Docker Compose

```yaml
version: '3.8'

services:
  roulette-api:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_HOST=postgres
      - DB_USERNAME=casino_user
      - DB_PASSWORD=casino_password
      - REDIS_HOST=redis
    depends_on:
      - postgres
      - redis
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=casino_roulette
      - POSTGRES_USER=casino_user
      - POSTGRES_PASSWORD=casino_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    restart: unless-stopped
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data

volumes:
  postgres_data:
  redis_data:
```

## Production Deployment

### Environment Variables for Production

```bash
# Database
export DB_HOST=prod-db.casino.com
export DB_USERNAME=roulette_prod_user
export DB_PASSWORD=secure_production_password
export DB_NAME=roulette_production

# Application
export SPRING_PROFILES_ACTIVE=production
export SERVER_PORT=8080
export JVM_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

# Security
export ENCRYPTION_KEY=base64_encoded_encryption_key
export API_RATE_LIMIT=100

# Monitoring
export LOG_LEVEL_ROOT=WARN
export LOG_LEVEL_CASINO=INFO
export LOG_FILE=/var/log/roulette/application.log

# Redis
export REDIS_HOST=prod-redis.casino.com
export REDIS_PASSWORD=redis_production_password
```

### Kubernetes Configuration

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: roulette-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: roulette-api
  template:
    metadata:
      labels:
        app: roulette-api
    spec:
      containers:
      - name: roulette-api
        image: casino/roulette-api:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DB_HOST
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: host
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /api/actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /api/actuator/health
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
```

### Monitoring Configuration

```yaml
# Prometheus metrics
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    metrics:
      enabled: true
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
```

## Configuration Validation

### Startup Checks

The application performs these validation checks on startup:

1. **Database Connection**: Verifies database connectivity
2. **Migration Status**: Checks Flyway migration status
3. **Required Tables**: Validates all required tables exist
4. **Configuration Values**: Validates critical configuration values
5. **External Dependencies**: Checks Redis connectivity (if enabled)

### Health Checks

Available health check endpoints:

- `/api/actuator/health` - Overall application health
- `/api/actuator/health/db` - Database health
- `/api/actuator/health/redis` - Redis health (if enabled)
- `/api/actuator/info` - Application information

### Configuration Troubleshooting

**Common Issues:**

1. **Database Connection Failed**
   ```
   Error: Could not connect to database
   Solution: Check DB_HOST, DB_USERNAME, DB_PASSWORD environment variables
   ```

2. **Migration Failed**
   ```
   Error: Flyway migration failed
   Solution: Check database permissions and migration scripts
   ```

3. **Port Already in Use**
   ```
   Error: Port 8080 is already in use
   Solution: Change SERVER_PORT environment variable
   ```

4. **Out of Memory**
   ```
   Error: OutOfMemoryError
   Solution: Increase JVM_OPTS heap size (-Xmx parameter)
   ```

For additional support, contact the development team at dev@casino.com.