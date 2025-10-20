# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a TDD (Test-Driven Development) practice project for implementing a user point management system. The project is a Spring Boot 3.2.0 application using Java 17 and follows TDD principles.

**Project Name**: hhplus-tdd-jvm
**Package**: io.hhplus.tdd
**Build Tool**: Gradle with Kotlin DSL

## Building and Running

### Build Commands
```bash
# Build the project
./gradlew build

# Build without tests
./gradlew build -x test

# Clean and build
./gradlew clean build

# Run the application
./gradlew bootRun
```

### Testing
```bash
# Run all tests
./gradlew test

# Run tests with JaCoCo coverage report
./gradlew test jacocoTestReport

# Run a specific test class
./gradlew test --tests "ClassName"

# Run a specific test method
./gradlew test --tests "ClassName.methodName"
```

Note: Tests are configured with `ignoreFailures = true` in build.gradle.kts, so the build will continue even if tests fail.

### Other Common Commands
```bash
# View dependencies
./gradlew dependencies

# Check for dependency updates
./gradlew dependencyUpdates
```

## Architecture and Code Structure

### Layer Architecture
The project follows a standard Spring Boot architecture with these key layers:

1. **Controller Layer** (`io.hhplus.tdd.point.PointController`)
   - REST endpoints for point operations
   - Currently contains TODO stubs for implementation
   - Endpoints: GET /{id}, GET /{id}/histories, PATCH /{id}/charge, PATCH /{id}/use

2. **Database Layer** (`io.hhplus.tdd.database`)
   - **IMPORTANT**: Table classes (`UserPointTable`, `PointHistoryTable`) must NOT be modified
   - These are mock database implementations with artificial throttling (200-300ms random delays)
   - Use only the public APIs provided by these tables
   - Data stored in-memory using HashMap/ArrayList

3. **Domain Models** (`io.hhplus.tdd.point`)
   - `UserPoint`: Java record for user point data (id, point, updateMillis)
   - `PointHistory`: Java record for transaction history (id, userId, amount, type, updateMillis)
   - `TransactionType`: Enum for CHARGE/USE operations

4. **Error Handling** (`io.hhplus.tdd.ApiControllerAdvice`)
   - Global exception handler using @RestControllerAdvice
   - Returns `ErrorResponse` with code and message

### Database Tables API

**UserPointTable**:
- `selectById(Long id)`: Returns UserPoint or empty if not found
- `insertOrUpdate(long id, long amount)`: Saves user point data

**PointHistoryTable**:
- `insert(long userId, long amount, TransactionType type, long updateMillis)`: Records transaction
- `selectAllByUserId(long userId)`: Returns all transactions for a user

Both tables include artificial throttling to simulate real database latency.

### Implementation Notes

1. **TDD Approach**: Write tests first before implementing features in PointController
2. **Service Layer**: Should be created to handle business logic (not yet implemented)
3. **Concurrency**: Consider thread-safety when implementing charge/use operations
4. **Validation**: Implement proper validation for point amounts (negative checks, overflow, etc.)
5. **Transaction History**: Update PointHistoryTable whenever points are charged or used

### Technology Stack
- Spring Boot 3.2.0
- Spring Web (REST APIs)
- Lombok (reduce boilerplate)
- JUnit 5 (testing)
- JaCoCo (code coverage)
- Java 17 with Records
