# User Service Test Suite

This directory contains comprehensive test cases for the user-service microservice.

## Test Structure

### 1. Unit Tests

#### AuthServiceTest
- **Location**: `service/AuthServiceTest.java`
- **Purpose**: Tests business logic in isolation
- **Coverage**:
  - User signup (success and failure scenarios)
  - User login (success and failure scenarios)
  - Password validation
  - Role assignment and validation
  - Kafka event publishing
  - Input validation and error handling

#### JwtServiceTest
- **Location**: `service/JwtServiceTest.java`
- **Purpose**: Tests JWT token generation and validation
- **Coverage**:
  - Access token generation
  - Token validation
  - Claims extraction (user ID, email, roles)
  - Token expiration handling
  - Invalid token handling
  - Edge cases (null, empty, malformed tokens)

#### PasswordUtilTest
- **Location**: `utils/PasswordUtilTest.java`
- **Purpose**: Tests password hashing and verification utilities
- **Coverage**:
  - Password hashing with BCrypt
  - Password verification
  - Salt generation (different hashes for same password)
  - Special characters and Unicode support
  - Edge cases (null, empty passwords)
  - Invalid hash format handling

### 2. Repository Tests

#### UserRepositoryTest
- **Location**: `repository/UserRepositoryTest.java`
- **Purpose**: Tests JPA repository operations with H2 database
- **Coverage**:
  - User CRUD operations
  - Email-based queries (findByEmail, existsByEmail)
  - User-role relationships
  - Unique constraint validation
  - Case sensitivity testing

#### RoleRepositoryTest
- **Location**: `repository/RoleRepositoryTest.java`
- **Purpose**: Tests role repository operations
- **Coverage**:
  - Role CRUD operations
  - Name-based queries (findByName, existsByName)
  - Unique constraint validation
  - Case sensitivity testing

### 3. Controller Tests

#### AuthControllerTest
- **Location**: `controller/AuthControllerTest.java`
- **Purpose**: Tests REST API endpoints with mocked services
- **Coverage**:
  - POST /auth/signup (success and error scenarios)
  - POST /auth/login (success and error scenarios)
  - Request validation
  - Content type validation
  - Error response formatting

### 4. Integration Tests

#### AuthIntegrationTest
- **Location**: `integration/AuthIntegrationTest.java`
- **Purpose**: End-to-end testing with real database
- **Coverage**:
  - Complete authentication flow (signup → login)
  - Database persistence verification
  - Role creation and assignment
  - Error scenarios with real data
  - Multi-role user handling

### 5. Exception Handling Tests

#### GlobalExceptionHandlerTest
- **Location**: `exception/GlobalExceptionHandlerTest.java`
- **Purpose**: Tests error handling and response formatting
- **Coverage**:
  - BadRequestException handling
  - ResourceNotFoundException handling
  - Generic exception handling
  - Proper HTTP status codes and error messages

## Test Dependencies

The following dependencies have been added to support testing:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

## Running Tests

### All Tests
```bash
mvn test
```

### Specific Test Class
```bash
mvn test -Dtest=AuthServiceTest
```

### Integration Tests Only
```bash
mvn test -Dtest=*IntegrationTest
```

### Unit Tests Only
```bash
mvn test -Dtest=*Test -Dtest=!*IntegrationTest
```

## Test Configuration

### Test Profile
Tests use the `test` profile with the following configuration:
- H2 in-memory database for fast testing
- Disabled external dependencies (Eureka, Config Server)
- Mocked Kafka for unit tests
- Debug logging for troubleshooting

### Database Setup
- H2 database is automatically created and destroyed for each test
- Repository tests use `@DataJpaTest` for focused database testing
- Integration tests use `@Transactional` for automatic rollback

## Test Features

### 1. Comprehensive Mocking
- Service layer dependencies mocked in unit tests
- Kafka templates mocked to avoid external dependencies
- Password utilities mocked for predictable testing

### 2. Real Database Testing
- Repository tests use H2 for authentic JPA testing
- Integration tests verify complete data flow
- Transaction management and rollback testing

### 3. Security Testing
- Password hashing and verification
- JWT token generation and validation
- Authentication and authorization flows

### 4. Error Scenario Testing
- Invalid credentials
- Duplicate user registration
- Missing required fields
- Invalid roles and permissions

## Coverage Areas

✅ **Authentication**: User signup and login flows
✅ **Authorization**: Role-based access control
✅ **Security**: Password hashing and JWT tokens
✅ **Data Persistence**: User and role management
✅ **API Endpoints**: REST controller testing
✅ **Error Handling**: Exception scenarios and responses
✅ **Integration**: End-to-end workflow testing
✅ **Validation**: Input validation and business rules

## Best Practices Implemented

1. **Test Isolation**: Each test is independent with clean setup/teardown
2. **Realistic Data**: Tests use realistic user data and scenarios
3. **Edge Cases**: Comprehensive testing of boundary conditions
4. **Security Focus**: Emphasis on authentication and authorization testing
5. **Performance**: Fast tests with in-memory database
6. **Maintainability**: Clear test structure and documentation

## Key Test Scenarios

### Authentication Flow
1. User registration with valid data
2. User login with correct credentials
3. JWT token generation and validation
4. Password hashing and verification

### Error Handling
1. Duplicate email registration
2. Invalid login credentials
3. Missing required fields
4. Invalid role assignments

### Security
1. Password strength and hashing
2. JWT token expiration
3. Role-based access control
4. Input sanitization

### Data Integrity
1. User-role relationships
2. Unique constraints
3. Database transactions
4. Data validation
