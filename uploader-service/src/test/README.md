# Uploader Service Test Suite

This directory contains comprehensive test cases for the uploader-service microservice.

## Test Structure

### 1. Unit Tests

#### FileUploadControllerTest
- **Location**: `controller/FileUploadControllerTest.java`
- **Purpose**: Tests REST API endpoints in isolation
- **Coverage**:
  - File upload endpoint (success and failure scenarios)
  - Request validation and error handling
  - Different file types and sizes
  - Edge cases (empty files, special characters)
  - HTTP status code validation

#### CloudinaryFileUploaderServiceTest
- **Location**: `service/CloudinaryFileUploaderServiceTest.java`
- **Purpose**: Tests Cloudinary integration service
- **Coverage**:
  - Successful file uploads to Cloudinary
  - Error handling for Cloudinary failures
  - Different file formats and sizes
  - Exception propagation and wrapping
  - Mock verification of Cloudinary API calls

#### GoogleCloudStorageFileUploaderServiceTest
- **Location**: `service/GoogleCloudStorageFileUploaderServiceTest.java`
- **Purpose**: Tests Google Cloud Storage integration service
- **Coverage**:
  - Successful file uploads to GCS
  - Unique filename generation
  - Error handling for GCS failures
  - Different file formats and sizes
  - Blob creation and URL generation

#### UploaderConfigTest
- **Location**: `config/UploaderConfigTest.java`
- **Purpose**: Tests configuration bean creation
- **Coverage**:
  - Cloudinary bean configuration
  - Google Cloud Storage bean configuration
  - Configuration validation
  - Error handling for invalid configurations
  - Edge cases (null, empty, malformed values)

### 2. Exception Handling Tests

#### GlobalExceptionHandlerTest
- **Location**: `exception/GlobalExceptionHandlerTest.java`
- **Purpose**: Tests error handling and response formatting
- **Coverage**:
  - IOException handling
  - RuntimeException handling
  - IllegalArgumentException handling
  - SecurityException handling
  - MaxUploadSizeExceededException handling
  - Proper HTTP status codes and error messages

### 3. Integration Tests

#### FileUploadIntegrationTest
- **Location**: `integration/FileUploadIntegrationTest.java`
- **Purpose**: End-to-end testing with mocked services
- **Coverage**:
  - Complete file upload flow
  - Multiple file format handling
  - Error scenario testing
  - Large file handling
  - Special character handling

### 4. Performance Tests

#### FileUploadPerformanceTest
- **Location**: `performance/FileUploadPerformanceTest.java`
- **Purpose**: Performance and load testing
- **Coverage**:
  - Upload time limits for different file sizes
  - Concurrent upload handling
  - Memory pressure testing
  - Performance degradation detection
  - Recovery from temporary failures

### 5. Application Tests

#### UploaderServiceApplicationTests
- **Location**: `UploaderServiceApplicationTests.java`
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
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
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
mvn test -Dtest=FileUploadControllerTest
mvn test -Dtest=CloudinaryFileUploaderServiceTest
mvn test -Dtest=GoogleCloudStorageFileUploaderServiceTest
```

### Test Categories
```bash
# Unit tests only
mvn test -Dtest=*Test -Dtest=!*IntegrationTest -Dtest=!*PerformanceTest

# Integration tests only
mvn test -Dtest=*IntegrationTest

# Performance tests only
mvn test -Dtest=*PerformanceTest
```

## Test Configuration

### Test Profile
Tests use the `test` profile with the following configuration:
- Disabled external dependencies (Eureka, Config Server)
- Mock configurations for Cloudinary and GCS
- Debug logging for troubleshooting

### Test Properties
Key test properties in `application-test.yml`:
```yaml
spring:
  cloud:
    config:
      enabled: false
eureka:
  client:
    enabled: false
cloudinary:
  cloud-name: test-cloud
  api-key: test-key
  api-secret: test-secret
```

## Test Features

### 1. Comprehensive Mocking
- External service dependencies mocked
- No real API calls during testing
- Predictable test behavior

### 2. Error Scenario Testing
- Network failures
- Invalid configurations
- File processing errors
- Security exceptions

### 3. Performance Validation
- Upload time limits
- Concurrent request handling
- Memory usage optimization
- Throughput measurement

### 4. Edge Case Coverage
- Empty files
- Large files (up to 10MB)
- Special characters in filenames
- Various file formats
- Null and invalid inputs

## Coverage Areas

✅ **File Upload API**: Complete REST endpoint testing
✅ **Service Layer**: Both Cloudinary and GCS service testing
✅ **Configuration**: Bean creation and validation
✅ **Error Handling**: Comprehensive exception scenarios
✅ **Integration**: End-to-end workflow testing
✅ **Performance**: Load and stress testing
✅ **Security**: Error message sanitization
✅ **Validation**: Input validation and business rules

## Best Practices Implemented

1. **Test Isolation**: Each test is independent with clean setup/teardown
2. **Realistic Data**: Tests use realistic file data and scenarios
3. **Mock Verification**: Proper verification of mock interactions
4. **Performance Focus**: Emphasis on upload performance testing
5. **Error Coverage**: Comprehensive error scenario testing
6. **Maintainability**: Clear test structure and documentation

## Key Test Scenarios

### File Upload Flow
1. Valid file upload with success response
2. Invalid file handling with error response
3. Large file upload performance
4. Concurrent upload handling

### Error Handling
1. Service unavailable scenarios
2. Invalid configuration handling
3. Network timeout simulation
4. File processing failures

### Performance
1. Upload time validation
2. Concurrent request handling
3. Memory usage optimization
4. Throughput measurement

### Security
1. File type validation
2. Size limit enforcement
3. Error message sanitization
4. Input validation

## Test Results Summary

The test suite provides comprehensive coverage of:
- **API Layer**: REST endpoint validation
- **Service Layer**: File upload service testing
- **Configuration**: Bean and property validation
- **Error Handling**: Exception scenario coverage
- **Performance**: Load and stress testing
- **Integration**: End-to-end workflow validation

This ensures the uploader-service is robust, performant, and reliable for production use.
