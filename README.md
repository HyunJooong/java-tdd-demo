# 포인트 관리 시스템 (Point Management System)

TDD(Test-Driven Development) 방식으로 구현한 사용자 포인트 충전/사용 시스템입니다.

## 프로젝트 개요

- **프로젝트명**: hhplus-tdd-jvm
- **패키지**: io.hhplus.tdd
- **빌드 도구**: Gradle with Kotlin DSL
- **Java 버전**: 17
- **Spring Boot 버전**: 3.2.0

## 주요 기능

### 1. 포인트 조회
- 특정 사용자의 현재 포인트 잔액 조회
- Endpoint: `GET /point/{id}`

### 2. 포인트 충전
- 사용자 포인트 충전
- Endpoint: `PATCH /point/{id}/charge`
- **정책1**: 100만원 이상 충전 불가

### 3. 포인트 사용
- 사용자 포인트 사용 (결제)
- Endpoint: `PATCH /point/{id}/use`
- **정책2**: 10,000원 이하 가격에는 포인트 사용 불가
- **정책3**: 결제 금액의 최대 50%까지만 포인트 사용 가능

### 4. 포인트 내역 조회
- 사용자의 포인트 충전/사용 내역 조회
- Endpoint: `GET /point/{id}/histories`

## 기술 스택

- **Spring Boot 3.2.0**
- **Java 17** (Record 사용)
- **Spring Web** (REST API)
- **Lombok** (코드 간소화)
- **JUnit 5** (단위 테스트)
- **AssertJ** (테스트 검증)
- **JaCoCo** (코드 커버리지)

## 프로젝트 구조

```
src
├── main
│   └── java
│       └── io.hhplus.tdd
│           ├── controller        # REST API 컨트롤러
│           │   └── PointController.java
│           ├── service          # 비즈니스 로직
│           │   ├── PointService.java
│           │   └── PointServiceImpl.java
│           ├── database         # 데이터 레이어 (In-Memory)
│           │   ├── UserPointTable.java
│           │   └── PointHistoryTable.java
│           ├── point            # 도메인 모델
│           │   ├── UserPoint.java
│           │   ├── PointHistory.java
│           │   └── TransactionType.java
│           └── exception        # 예외 처리
│               └── InsufficientPointException.java
└── test
    └── java
        └── io.hhplus.tdd
            ├── controller       # 컨트롤러 테스트
            ├── service         # 서비스 단위 테스트
            │   ├── PointServiceTest.java
            │   └── PointServiceConcurrencyTest.java
            └── integration     # 통합 테스트
                └── PointIntegrationTest.java
```

## 빌드 및 실행

### 프로젝트 빌드
```bash
# 전체 빌드
./gradlew build
```

### 테스트 실행
```bash
# 전체 테스트
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "PointServiceTest"

# 특정 테스트 메서드 실행
./gradlew test --tests "PointServiceTest.charge_success"

# 동시성 테스트 실행
./gradlew test --tests "PointServiceConcurrencyTest"

# 통합 테스트 실행
./gradlew test --tests "PointIntegrationTest"

# 커버리지 리포트 생성
./gradlew test jacocoTestReport
```

## API 명세

### 1. 포인트 조회
```http
GET /point/{id}
```
**Response:**
```json
{
  "id": 1,
  "point": 5000,
  "updateMillis": 1234567890,
  "cost": 0
}
```


## 동시성 제어

### 비관적 락 (Pessimistic Lock) 구현
동일한 사용자에 대한 동시 요청을 안전하게 처리하기 위해 **ReentrantLock**을 사용한 비관적 락을 구현했습니다.

**특징:**
- 사용자별 Lock 관리 (`ConcurrentHashMap<Long, Lock>`)
- 충전/사용 작업 전 Lock 획득
- 다른 사용자 간 독립적 처리 (성능 최적화)
- Race Condition 방지

**구현 코드:**
```java
private final ConcurrentHashMap<Long, Lock> userLocks = new ConcurrentHashMap<>();

private Lock getUserLock(long userId) {
    return userLocks.computeIfAbsent(userId, id -> new ReentrantLock());
}

public UserPoint charge(long id, long amount) {
    Lock lock = getUserLock(id);
    lock.lock();
    try {
        // 포인트 충전 로직
    } finally {
        lock.unlock();
    }
}
```

## 테스트 전략

### 1. 단위 테스트 (PointServiceTest)
- 포인트 조회, 충전, 사용, 내역 조회
- 각종 정책 검증
- 예외 케이스 처리

### 2. 동시성 테스트 (PointServiceConcurrencyTest)
- 동일 사용자 동시 충전
- 동일 사용자 동시 사용
- 충전/사용 혼합 시나리오
- 여러 사용자 동시 작업
- 포인트 부족 시 동시 사용

### 3. 통합 테스트 (PointIntegrationTest)
- 전체 레이어 통합 테스트
- HTTP 요청/응답 검증
- 실제 시나리오 재현

## 비즈니스 정책

### 정책1: 포인트 충전 제한
- 1회 충전 시 100만원 이상 충전 불가
- 위반 시 `InsufficientPointException` 발생

### 정책2: 최소 결제 금액
- 10,000원 이하 가격에는 포인트 사용 불가
- 위반 시 `InsufficientPointException` 발생

### 정책3: 포인트 사용 비율 제한
- 결제 금액의 최대 50%까지만 포인트 사용 가능
- 예: 20,000원 결제 시 최대 10,000원까지 포인트 사용 가능
- 위반 시 `InsufficientPointException` 발생

## TDD 개발 프로세스

이 프로젝트는 TDD(Test-Driven Development) 방식으로 개발되었습니다:

1. **RED**: 실패하는 테스트 작성
2. **GREEN**: 테스트를 통과하는 최소한의 코드 작성
3. **REFACTOR**: 코드 개선 및 리팩토링

## 예외 처리

### InsufficientPointException
- 포인트 부족
- 정책 위반 (100만원 이상 충전, 10,000원 이하 사용, 50% 초과 사용)

### ApiControllerAdvice
- 전역 예외 처리
- 일관된 에러 응답 형식

