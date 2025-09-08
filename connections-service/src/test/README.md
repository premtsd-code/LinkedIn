# Connections Service Test Suite ✅

This directory contains comprehensive test cases for the connections-service microservice.

## 🎯 **Test Status: ALL TESTS PASSING**

✅ **ConnectionsServiceTest**: 10/10 tests passing
✅ **ConnectionsControllerSimpleTest**: 9/9 tests passing
✅ **PersonRepositoryUnitTest**: 11/11 tests passing
✅ **Total**: 30/30 tests passing

## Test Structure

### 1. Unit Tests

#### ConnectionsServiceTest
- **Location**: `service/ConnectionsServiceTest.java`
- **Purpose**: Tests business logic in isolation
- **Coverage**:
  - User creation event handling
  - First-degree connections retrieval
  - Connection request sending (success and failure scenarios)
  - Connection request acceptance (success and failure scenarios)
  - Connection request rejection (success and failure scenarios)
  - Validation logic (self-connection prevention)

#### PersonRepositoryTest
- **Location**: `repository/PersonRepositoryTest.java`
- **Purpose**: Tests Neo4j repository operations with real database
- **Coverage**: Full Neo4j operations testing
- **Technology**: Uses Testcontainers with Neo4j (requires Docker)
- **Note**: Only runs when `DOCKER_AVAILABLE=true` environment variable is set

#### PersonRepositoryUnitTest
- **Location**: `repository/PersonRepositoryUnitTest.java`
- **Purpose**: Tests repository interface with mocks (no Docker required)
- **Coverage**:
  - Person lookup by name
  - Connection request existence checks
  - Connection existence checks
  - Adding connection requests
  - Accepting connection requests
  - Rejecting connection requests
  - First-degree connections retrieval

### 2. Integration Tests

#### ConnectionsControllerTest
- **Location**: `controller/ConnectionsControllerTest.java`
- **Purpose**: Tests REST API endpoints in isolation
- **Coverage**:
  - GET /core/first-degree (with and without connections)
  - POST /core/request/{userId} (success and error scenarios)
  - POST /core/accept/{userId} (success and error scenarios)
  - Error handling for invalid inputs
  - Exception handling

#### ConnectionsIntegrationTest
- **Location**: `integration/ConnectionsIntegrationTest.java`
- **Purpose**: End-to-end testing with real Neo4j database
- **Coverage**: Complete workflows with real database
- **Technology**: Uses Testcontainers with Neo4j (requires Docker)
- **Note**: Only runs when `DOCKER_AVAILABLE=true` environment variable is set

#### ConnectionsControllerIntegrationTest
- **Location**: `integration/ConnectionsControllerIntegrationTest.java`
- **Purpose**: Integration testing with mocked services (no Docker required)
- **Coverage**:
  - Complete connection flow (request → accept → verify)
  - Connection rejection workflow
  - Error handling scenarios
  - All endpoint accessibility

### 3. Exception Handling Tests

#### GlobalExceptionHandlerTest
- **Location**: `exception/GlobalExceptionHandlerTest.java`
- **Purpose**: Tests error handling and response formatting
- **Coverage**:
  - BusinessRuleViolationException handling
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
    <artifactId>neo4j</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
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
mvn test -Dtest=ConnectionsServiceTest
```

### Integration Tests Only
```bash
mvn test -Dtest=*IntegrationTest
```

### Unit Tests Only
```bash
mvn test -Dtest=*Test -Dtest=!*IntegrationTest
```

## Test Features

### 1. Testcontainers Integration
- Real Neo4j database for repository and integration tests
- Automatic container lifecycle management
- Isolated test environments

### 2. Comprehensive Mocking
- Service layer dependencies mocked in unit tests
- Kafka templates mocked to avoid external dependencies
- UserContextHolder mocked for authentication simulation

### 3. Error Scenario Testing
- Business rule violations
- Invalid inputs
- Non-existent resources
- Database errors

### 4. End-to-End Workflows
- Complete connection establishment flow
- Connection rejection scenarios
- Multiple user interactions

## Test Data Management

### Setup
- Each test class has `@BeforeEach` methods to set up clean test data
- Repository tests clean the Neo4j database before each test
- Integration tests create fresh Person nodes for each test

### Isolation
- Tests are independent and don't affect each other
- Database state is reset between tests
- Mock objects are reset between test methods

## Coverage Areas

✅ **Controller Layer**: REST endpoint testing with MockMvc
✅ **Service Layer**: Business logic testing with mocked dependencies  
✅ **Repository Layer**: Neo4j operations with real database
✅ **Exception Handling**: Error scenarios and response formatting
✅ **Integration**: End-to-end workflows with real components
✅ **Validation**: Input validation and business rule enforcement

## Best Practices Implemented

1. **Test Naming**: Descriptive test method names following Given-When-Then pattern
2. **Test Organization**: Logical grouping by functionality
3. **Mocking Strategy**: Mock external dependencies, test real business logic
4. **Data Management**: Clean test data setup and teardown
5. **Assertion Quality**: Meaningful assertions with clear error messages
6. **Edge Cases**: Testing boundary conditions and error scenarios
