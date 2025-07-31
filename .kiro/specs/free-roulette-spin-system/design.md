# Design Document

## Overview

The free roulette spin system is a Spring Boot backend application that manages user rewards, roulette gameplay, and letter collection mechanics for a casino platform. The system provides RESTful APIs for user authentication, mission management, roulette spinning, and reward claiming.

The architecture follows Domain-Driven Design principles with clear separation between business logic, data access, and API layers. The system uses JPA for database operations, Spring Security for authentication, and implements transactional consistency for all financial operations.

## Architecture

### High-Level Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Vue Frontend  │────│  Spring Boot    │────│   PostgreSQL    │
│                 │    │   Backend       │    │   Database      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                       ┌─────────────────┐
                       │   Redis Cache   │
                       │  (Optional)     │
                       └─────────────────┘
```

### Layer Architecture

- **Controller Layer**: REST API endpoints for frontend communication
- **Service Layer**: Business logic implementation and transaction management  
- **Repository Layer**: Data access using Spring Data JPA
- **Entity Layer**: JPA entities representing database tables
- **DTO Layer**: Data transfer objects for API communication

## Components and Interfaces

### Core Entities

#### User Entity
```java
@Entity
public class User {
    private Long id; // This will be the external user ID from main system
    private BigDecimal cashBalance;
    private Integer availableSpins;
    private Boolean firstDepositBonusUsed;
    private LocalDateTime lastDailyLogin;
    private LocalDateTime createdAt;
}
```

#### DepositMission Entity
```java
@Entity  
public class DepositMission {
    private Long id;
    private String name;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Integer spinsGranted;
    private Integer maxClaims;
    private Boolean active;
}
```

#### UserMissionProgress Entity
```java
@Entity
public class UserMissionProgress {
    private Long id;
    private Long userId;
    private Long missionId;
    private Integer claimsUsed;
    private LocalDateTime lastClaimDate;
}
```

#### RouletteSlot Entity
```java
@Entity
public class RouletteSlot {
    private Long id;
    private String type; // CASH or LETTER
    private String value; // Amount for cash, letter for letter type
    private Integer weight;
    private Boolean active;
}
```

#### LetterCollection Entity
```java
@Entity
public class LetterCollection {
    private Long id;
    private Long userId;
    private String letter;
    private Integer count;
}
```

#### LetterWord Entity
```java
@Entity
public class LetterWord {
    private Long id;
    private String word;
    private String requiredLetters; // JSON: {"H":1,"A":1,"P":2,"Y":1}
    private BigDecimal rewardAmount;
    private Boolean active;
}
```

### Service Interfaces

#### UserService
- `getOrCreateUser(Long userId): User`
- `updateCashBalance(Long userId, BigDecimal amount): void`
- `grantDailyLoginSpin(Long userId): boolean`
- `processFirstDepositBonus(Long userId): void`

#### MissionService  
- `getAvailableMissions(Long userId): List<MissionDTO>`
- `claimMissionReward(Long userId, Long missionId): void`
- `processDepositMissions(Long userId, BigDecimal amount): void`

#### RouletteService
- `spinRoulette(Long userId): SpinResultDTO`
- `getRouletteConfiguration(): List<RouletteSlotDTO>`
- `updateRouletteSlots(List<RouletteSlotDTO> slots): void`

#### LetterService
- `addLetterToCollection(Long userId, String letter): void`
- `getUserLetterCollection(Long userId): List<LetterCollectionDTO>`
- `getAvailableWords(): List<LetterWordDTO>`
- `claimWordBonus(Long userId, Long wordId): void`

### API Endpoints

#### Mission Controller  
- `GET /api/missions` - Get available missions for user
- `POST /api/missions/{missionId}/claim` - Claim mission reward
- `GET /api/missions/progress` - Get user mission progress

#### Roulette Controller
- `POST /api/roulette/spin` - Spin the roulette wheel
- `GET /api/roulette/slots` - Get roulette configuration
- `PUT /api/roulette/slots` - Update roulette slots (admin)

#### Letter Controller
- `GET /api/letters/collection` - Get user letter collection
- `GET /api/letters/words` - Get available words for collection
- `POST /api/letters/words/{wordId}/claim` - Claim word bonus

#### Deposit Controller
- `POST /api/deposits` - Process user deposit
- `GET /api/deposits/history` - Get deposit history##
 Data Models

### Database Schema Design

#### Users Table
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY, -- External user ID from main system
    cash_balance DECIMAL(10,2) DEFAULT 0.00,
    available_spins INTEGER DEFAULT 0,
    first_deposit_bonus_used BOOLEAN DEFAULT FALSE,
    last_daily_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Deposit Missions Table
```sql
CREATE TABLE deposit_missions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    min_amount DECIMAL(10,2) NOT NULL,
    max_amount DECIMAL(10,2),
    spins_granted INTEGER NOT NULL,
    max_claims INTEGER NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### User Mission Progress Table
```sql
CREATE TABLE user_mission_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    mission_id BIGINT REFERENCES deposit_missions(id),
    claims_used INTEGER DEFAULT 0,
    last_claim_date TIMESTAMP,
    UNIQUE(user_id, mission_id)
);
```

#### Roulette Slots Table
```sql
CREATE TABLE roulette_slots (
    id BIGSERIAL PRIMARY KEY,
    slot_type VARCHAR(10) NOT NULL, -- 'CASH' or 'LETTER'
    slot_value VARCHAR(50) NOT NULL, -- Amount or letter
    weight INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Letter Collections Table
```sql
CREATE TABLE letter_collections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    letter CHAR(1) NOT NULL,
    count INTEGER DEFAULT 0,
    UNIQUE(user_id, letter)
);
```

#### Letter Words Table
```sql
CREATE TABLE letter_words (
    id BIGSERIAL PRIMARY KEY,
    word VARCHAR(50) NOT NULL,
    required_letters TEXT NOT NULL, -- JSON format
    reward_amount DECIMAL(10,2) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Transaction Log Table
```sql
CREATE TABLE transaction_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### DTO Models

#### SpinResultDTO
```java
public class SpinResultDTO {
    private String type; // "CASH" or "LETTER"
    private String value; // Amount or letter
    private BigDecimal cashWon;
    private String letterWon;
    private Integer remainingSpins;
}
```

#### MissionDTO
```java
public class MissionDTO {
    private Long id;
    private String name;
    private String description;
    private Integer spinsAvailable;
    private Boolean canClaim;
    private Integer claimsUsed;
    private Integer maxClaims;
}
```

#### LetterCollectionDTO
```java
public class LetterCollectionDTO {
    private String letter;
    private Integer count;
}
```

#### LetterWordDTO
```java
public class LetterWordDTO {
    private Long id;
    private String word;
    private Map<String, Integer> requiredLetters;
    private BigDecimal rewardAmount;
    private Boolean canClaim;
}
```

## Error Handling

### Exception Hierarchy
- `BusinessException` - Base exception for business logic errors
- `InsufficientSpinsException` - When user has no spins available
- `MissionNotAvailableException` - When mission cannot be claimed
- `InsufficientLettersException` - When user lacks required letters
- `InvalidDepositAmountException` - When deposit amount is invalid

### Error Response Format
```json
{
    "error": {
        "code": "INSUFFICIENT_SPINS",
        "message": "You don't have enough spins available",
        "timestamp": "2024-01-15T10:30:00Z"
    }
}
```

### Global Exception Handler
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e);
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException e);
}
```

## Testing Strategy

### Unit Testing
- **Service Layer Tests**: Mock repository dependencies, test business logic
- **Repository Tests**: Use @DataJpaTest for database operations
- **Controller Tests**: Use @WebMvcTest for API endpoint testing
- **Entity Tests**: Validate JPA mappings and constraints

### Integration Testing
- **API Integration Tests**: Full request/response cycle testing
- **Database Integration Tests**: Test complex queries and transactions
- **Transaction Tests**: Verify ACID properties for financial operations

### Test Data Management
- Use @Sql annotations for test data setup
- Implement test fixtures for common scenarios
- Use TestContainers for database integration tests

### Performance Testing
- Load testing for roulette spin operations
- Concurrent user testing for mission claiming
- Database query performance optimization

## Security Considerations

### Authorization
- User ID validation from main system
- Role-based access control for admin functions
- Request validation and user context

### Data Protection
- Input validation and sanitization
- SQL injection prevention through JPA
- XSS protection in API responses

### Financial Transaction Security
- Transactional consistency for all balance operations
- Audit logging for all financial transactions
- Idempotency for critical operations
- Rate limiting for sensitive endpoints

### Configuration Security
- Environment-based configuration
- Secure storage of database credentials
- Integration security with main system