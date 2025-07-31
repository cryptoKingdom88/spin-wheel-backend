# Free Roulette Spin System

A Spring Boot backend application that manages user rewards, roulette gameplay, and letter collection mechanics for a casino platform.

## Project Structure

```
src/
├── main/
│   ├── java/com/casino/roulette/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST API controllers
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── entity/         # JPA entities
│   │   ├── exception/      # Custom exceptions
│   │   ├── repository/     # Data access repositories
│   │   ├── service/        # Business logic services
│   │   └── RouletteSpinSystemApplication.java
│   └── resources/
│       └── application.yml  # Main configuration
└── test/
    ├── java/com/casino/roulette/
    │   └── RouletteSpinSystemApplicationTests.java
    └── resources/
        └── application-test.yml  # Test configuration
```

## Dependencies

- Spring Boot Web
- Spring Boot Data JPA
- PostgreSQL Driver
- Spring Boot Validation
- Spring Boot Test
- H2 Database (for testing)

## Database Configuration

The application is configured to use PostgreSQL for production and H2 for testing.

### Environment Variables

- `DB_USERNAME`: Database username (default: casino_user)
- `DB_PASSWORD`: Database password (default: casino_password)

## Running the Application

```bash
mvn spring-boot:run
```

## Running Tests

```bash
mvn test
```

## API Base URL

The application runs on port 8080 with context path `/api`.