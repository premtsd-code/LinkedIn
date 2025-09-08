# Post Service Test Suite

This directory contains comprehensive test cases for the post-service microservice.

## Test Structure

### 1. Service Layer Tests

#### PostsServiceTest
- **Location**: `service/PostsServiceTest.java`
- **Purpose**: Tests post creation and retrieval business logic
- **Coverage**:
  - Post creation with and without images
  - File upload integration
  - Kafka event publishing
  - User context handling
  - Error scenarios and exception handling
  - Post retrieval by ID and user ID

#### PostLikeServiceTest
- **Location**: `service/PostLikeServiceTest.java`
- **Purpose**: Tests post like/unlike functionality
- **Coverage**:
  - Like post operations
  - Unlike post operations
  - Like count retrieval
  - Like status checking
  - Duplicate like prevention
  - Post existence validation

#### UploaderServiceWrapperTest
- **Location**: `service/UploaderServiceWrapperTest.java`
- **Purpose**: Tests file upload wrapper with resilience patterns
- **Coverage**:
  - File upload delegation
  - Error handling and propagation
  - Different file types and sizes
  - Edge cases (null, empty files)
  - Special characters in filenames

### 2. Controller Layer Tests

#### PostsControllerTest
- **Location**: `controller/PostsControllerTest.java`
- **Purpose**: Tests REST API endpoints for posts
- **Coverage**:
  - POST /posts - Create post with multipart data
  - GET /posts/{id} - Retrieve post by ID
  - GET /posts/users/{userId} - Get user's posts
  - Request validation and error handling
  - HTTP status code validation
  - JSON response structure validation

#### LikesControllerTest
- **Location**: `controller/LikesControllerTest.java`
- **Purpose**: Tests REST API endpoints for likes
- **Coverage**:
  - POST /likes/{postId} - Like a post
  - DELETE /likes/{postId} - Unlike a post
  - GET /likes/{postId}/count - Get like count
  - GET /likes/{postId}/status - Check like status
  - Error scenarios and status codes

### 3. Repository Layer Tests

#### PostsRepositoryTest
- **Location**: `repository/PostsRepositoryTest.java`
- **Purpose**: Tests JPA repository operations for posts
- **Coverage**:
  - CRUD operations (Create, Read, Update, Delete)
  - Custom query methods (findByUserId)
  - Ordering and sorting
  - Data persistence and retrieval
  - Edge cases and constraints

#### PostLikeRepositoryTest
- **Location**: `repository/PostLikeRepositoryTest.java`
- **Purpose**: Tests JPA repository operations for likes
- **Coverage**:
  - CRUD operations for likes
  - Custom query methods (findByPostIdAndUserId, countByPostId)
  - Unique constraint validation
  - Relationship integrity
  - Performance with large datasets

### 4. Integration Tests

#### PostServiceIntegrationTest
- **Location**: `integration/PostServiceIntegrationTest.java`
- **Purpose**: End-to-end testing of complete workflows
- **Coverage**:
  - Complete post creation and retrieval flow
  - Like/unlike workflow
  - Multi-user interactions
  - Database persistence verification
  - Error handling across layers
  - Real HTTP request/response testing

### 5. Application Tests

#### PostServiceApplicationTests
- **Location**: `PostServiceApplicationTests.java`
- **Purpose**: Spring Boot application context testing
- **Coverage**:
  - Application context loading
  - Bean creation and wiring
  - Configuration validation

## Test Dependencies

The following dependencies have been added to support comprehensive testing:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
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

### Specific Test Classes
```bash
mvn test -Dtest=PostsServiceTest
mvn test -Dtest=PostLikeServiceTest
mvn test -Dtest=PostsControllerTest
mvn test -Dtest=PostsRepositoryTest
```

### Test Categories
```bash
# Unit tests only (Service + Repository)
mvn test -Dtest=*ServiceTest,*RepositoryTest

# Controller tests only
mvn test -Dtest=*ControllerTest

# Integration tests only
mvn test -Dtest=*IntegrationTest
```

## Test Configuration

### Test Profile
Tests use the `test` profile with the following configuration:
- H2 in-memory database for fast testing
- Disabled external dependencies (Eureka, Config Server)
- Mock configurations for external services
- Debug logging for troubleshooting

### Test Properties
Key test properties in `application-test.yml`:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    hibernate:
      ddl-auto: create-drop
  cloud:
    config:
      enabled: false
eureka:
  client:
    enabled: false
```

## Test Features

### 1. Comprehensive Mocking
- External service dependencies mocked (UploaderClient)
- Kafka template mocked for event testing
- User context mocked for authentication
- No real external API calls during testing

### 2. Database Testing
- H2 in-memory database for fast execution
- @DataJpaTest for repository layer testing
- @Transactional for test isolation
- Automatic rollback after each test

### 3. Integration Testing
- Full Spring context loading
- Real HTTP request/response testing
- Database persistence verification
- Multi-layer interaction testing

### 4. Error Scenario Testing
- Network failures and timeouts
- Invalid input validation
- Resource not found scenarios
- Duplicate operation prevention

## Coverage Areas

✅ **Post Management**: Complete CRUD operations testing
✅ **Like System**: Like/unlike functionality with validation
✅ **File Upload**: Image upload integration testing
✅ **Event Publishing**: Kafka event testing
✅ **User Context**: Authentication and authorization
✅ **Data Persistence**: Repository and database testing
✅ **API Endpoints**: REST controller testing
✅ **Error Handling**: Comprehensive exception scenarios
✅ **Integration**: End-to-end workflow testing

## Best Practices Implemented

1. **Test Isolation**: Each test is independent with clean setup/teardown
2. **Realistic Data**: Tests use realistic post and user data
3. **Mock Verification**: Proper verification of mock interactions
4. **Database Testing**: Comprehensive repository testing with H2
5. **Error Coverage**: Extensive error scenario testing
6. **Performance Focus**: Tests designed for fast execution
7. **Maintainability**: Clear test structure and documentation

## Key Test Scenarios

### Post Management Flow
1. Create post with image upload
2. Create post without image
3. Retrieve post by ID
4. Get all posts for a user
5. Handle upload failures

### Like System Flow
1. Like a post successfully
2. Prevent duplicate likes
3. Unlike a post
4. Get like count
5. Check like status

### Error Handling
1. Post not found scenarios
2. Invalid user operations
3. File upload failures
4. Database constraint violations

### Integration Scenarios
1. Complete post creation to retrieval flow
2. Multi-user like interactions
3. Database persistence verification
4. Cross-layer error propagation

## Test Results Summary

The test suite provides comprehensive coverage of:
- **Service Layer**: Business logic validation
- **Controller Layer**: API endpoint testing
- **Repository Layer**: Data persistence testing
- **Integration Layer**: End-to-end workflow validation
- **Error Handling**: Exception scenario coverage

This ensures the post-service is robust, reliable, and ready for production use with comprehensive test coverage across all layers of the application.
