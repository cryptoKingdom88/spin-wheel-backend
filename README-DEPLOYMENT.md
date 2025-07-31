# Free Roulette Spin System - Quick Deployment Guide

## Quick Start

### Prerequisites
- Docker Engine 20.10+
- Docker Compose 2.0+
- Maven 3.6+ (for building)

### Development Deployment

1. **Clone and build**:
   ```bash
   git clone <repository-url>
   cd free-roulette-spin-system
   mvn clean package -DskipTests
   ```

2. **Start services**:
   ```bash
   docker-compose up -d
   ```

3. **Verify deployment**:
   ```bash
   ./scripts/test-deployment.sh
   ```

4. **Access services**:
   - API: http://localhost:8080/api
   - API Docs: http://localhost:8080/api/swagger-ui.html
   - Health: http://localhost:8080/api/actuator/health

### Production Deployment

1. **Prepare environment**:
   ```bash
   cp .env.example .env.prod
   # Edit .env.prod with production values
   ```

2. **Deploy**:
   ```bash
   ./scripts/deploy.sh prod
   ```

3. **Validate**:
   ```bash
   ./scripts/validate-deployment.sh
   ```

## Available Scripts

- `./scripts/deploy.sh [env]` - Deploy to environment (dev/prod)
- `./scripts/test-deployment.sh` - Test deployment functionality
- `./scripts/validate-deployment.sh` - Validate deployment configuration

## Documentation

- [Complete Deployment Guide](docs/DEPLOYMENT.md)
- [Configuration Guide](docs/CONFIGURATION.md)
- [API Integration Guide](docs/API_INTEGRATION_GUIDE.md)

## Support

For deployment issues, check the troubleshooting section in [DEPLOYMENT.md](docs/DEPLOYMENT.md).