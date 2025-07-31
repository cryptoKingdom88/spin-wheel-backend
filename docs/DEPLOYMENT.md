# Free Roulette Spin System - Deployment Guide

## Overview

This guide covers the deployment of the Free Roulette Spin System using Docker containers. The system consists of a Spring Boot API, PostgreSQL database, Redis cache, and optional monitoring components.

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- At least 4GB RAM available
- At least 10GB disk space

## Quick Start

### Development Environment

1. Clone the repository and navigate to the project directory
2. Start the development environment:

```bash
docker-compose up -d
```

This will start:
- API server on http://localhost:8080
- PostgreSQL on localhost:5432
- Redis on localhost:6379
- Nginx on http://localhost:80

### Production Environment

1. Create production environment file:

```bash
cp .env.example .env.prod
# Edit .env.prod with production values
```

2. Start production environment:

```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

## Environment Configuration

### Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `DB_HOST` | Database host | postgres | No |
| `DB_PORT` | Database port | 5432 | No |
| `DB_NAME` | Database name | casino_roulette | No |
| `DB_USERNAME` | Database username | casino_user | Yes |
| `DB_PASSWORD` | Database password | casino_password | Yes |
| `REDIS_HOST` | Redis host | redis | No |
| `REDIS_PORT` | Redis port | 6379 | No |
| `LOG_LEVEL_ROOT` | Root log level | INFO | No |
| `LOG_LEVEL_CASINO` | Application log level | INFO | No |
| `JVM_OPTS` | JVM options | -Xmx1g -Xms512m | No |

### Configuration Files

- `application.yml` - Base configuration
- `application-docker.yml` - Docker environment configuration
- `application-prod.yml` - Production environment configuration
- `application-test.yml` - Test environment configuration

## Database Setup

### Automatic Setup

The database is automatically initialized when the PostgreSQL container starts for the first time. The initialization includes:

1. Database creation (via `POSTGRES_DB` environment variable)
2. User creation (via `POSTGRES_USER` environment variable)
3. Initial schema setup (via `docker/init-db.sql`)
4. Flyway migrations (via Spring Boot startup)

### Manual Database Setup

If you need to set up the database manually:

1. Connect to PostgreSQL:

```bash
docker exec -it roulette-postgres psql -U casino_user -d casino_roulette
```

2. Verify tables are created:

```sql
\dt
```

3. Check sample data:

```sql
SELECT * FROM deposit_missions;
SELECT * FROM roulette_slots;
SELECT * FROM letter_words;
```

### Database Migrations

The system uses Flyway for database migrations. Migration files are located in `src/main/resources/db/migration/`:

- `V1__Create_initial_tables.sql` - Initial schema
- `V2__Create_indexes.sql` - Performance indexes
- `V3__Insert_sample_data.sql` - Sample data
- `V4__Enhanced_default_configurations.sql` - Enhanced configurations

To run migrations manually:

```bash
docker exec roulette-api java -jar app.jar --spring.flyway.migrate
```

### Database Backup and Restore

#### Backup

```bash
# Create backup directory
mkdir -p ./backups

# Backup database
docker exec roulette-postgres pg_dump -U casino_user casino_roulette > ./backups/backup_$(date +%Y%m%d_%H%M%S).sql
```

#### Restore

```bash
# Restore from backup
docker exec -i roulette-postgres psql -U casino_user casino_roulette < ./backups/backup_file.sql
```

## Service Management

### Starting Services

```bash
# Development
docker-compose up -d

# Production
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Specific service
docker-compose up -d roulette-api
```

### Stopping Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: This will delete data)
docker-compose down -v
```

### Viewing Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f roulette-api

# Last 100 lines
docker-compose logs --tail=100 roulette-api
```

### Service Health Checks

All services include health checks:

```bash
# Check service status
docker-compose ps

# Check API health
curl http://localhost:8080/api/actuator/health

# Check database health
docker exec roulette-postgres pg_isready -U casino_user
```

## Monitoring and Observability

### Prometheus Metrics

Metrics are available at:
- Application metrics: http://localhost:8080/api/actuator/prometheus
- Prometheus UI: http://localhost:9090

### Grafana Dashboard

Access Grafana at http://localhost:3000
- Username: admin
- Password: admin123

### Log Management

Logs are stored in:
- Container logs: `docker-compose logs`
- Application logs: `./logs/roulette-api.log`
- Database logs: PostgreSQL container logs

## Security Considerations

### Production Security

1. **Change default passwords**:
   - Database password
   - Grafana admin password

2. **Use environment variables**:
   - Never commit secrets to version control
   - Use `.env` files or external secret management

3. **Network security**:
   - Use internal Docker networks
   - Expose only necessary ports
   - Configure firewall rules

4. **SSL/TLS**:
   - Configure SSL certificates in Nginx
   - Use HTTPS in production

### SSL Configuration

1. Place SSL certificates in `./docker/ssl/`
2. Uncomment HTTPS configuration in `docker/nginx.conf`
3. Update certificate paths as needed

## Troubleshooting

### Common Issues

1. **Port conflicts**:
   ```bash
   # Check port usage
   netstat -tulpn | grep :8080
   
   # Change ports in docker-compose.yml
   ```

2. **Database connection issues**:
   ```bash
   # Check database logs
   docker-compose logs postgres
   
   # Test connection
   docker exec roulette-postgres pg_isready -U casino_user
   ```

3. **Memory issues**:
   ```bash
   # Check container memory usage
   docker stats
   
   # Adjust JVM_OPTS in docker-compose.yml
   ```

4. **Migration failures**:
   ```bash
   # Check migration status
   docker exec roulette-api java -jar app.jar --spring.flyway.info
   
   # Repair migrations
   docker exec roulette-api java -jar app.jar --spring.flyway.repair
   ```

### Performance Tuning

1. **Database**:
   - Adjust PostgreSQL configuration
   - Monitor connection pool usage
   - Optimize queries

2. **Application**:
   - Tune JVM parameters
   - Adjust thread pool sizes
   - Configure caching

3. **Redis**:
   - Monitor memory usage
   - Adjust eviction policies
   - Configure persistence

## Scaling

### Horizontal Scaling

1. **API scaling**:
   ```bash
   docker-compose up -d --scale roulette-api=3
   ```

2. **Load balancing**:
   - Configure Nginx upstream
   - Use external load balancer

3. **Database scaling**:
   - Read replicas
   - Connection pooling
   - Sharding (if needed)

### Vertical Scaling

1. **Increase resources**:
   - CPU limits
   - Memory limits
   - JVM heap size

2. **Optimize configuration**:
   - Connection pools
   - Thread pools
   - Cache sizes

## Maintenance

### Regular Tasks

1. **Log rotation**:
   - Configure logrotate
   - Monitor disk usage

2. **Database maintenance**:
   - Regular backups
   - Vacuum operations
   - Index maintenance

3. **Security updates**:
   - Update base images
   - Update dependencies
   - Security patches

### Monitoring Checklist

- [ ] Service health checks
- [ ] Database performance
- [ ] Memory usage
- [ ] Disk space
- [ ] Network connectivity
- [ ] Log errors
- [ ] Security alerts