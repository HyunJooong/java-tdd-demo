package io.hhplus.tdd.integration;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PointIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("통합 테스트: 포인트 충전 후 조회가 정상적으로 동작한다")
    void chargeAndGetUserPoint_integration() {
        // given: 유저 ID와 충전 금액
        long userId = 100L;
        long chargeAmount = 5000L;

        // when: 포인트 충전 (amount만 전송)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Long> chargeRequest = new HttpEntity<>(chargeAmount, headers);

        ResponseEntity<UserPoint> chargeResponse = restTemplate.exchange(
                "/point/{id}/charge",
                HttpMethod.PATCH,
                chargeRequest,
                UserPoint.class,
                userId
        );

        // then: 충전 성공
        assertThat(chargeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(chargeResponse.getBody()).isNotNull();
        assertThat(chargeResponse.getBody().id()).isEqualTo(userId);
        assertThat(chargeResponse.getBody().point()).isEqualTo(chargeAmount);
        assertThat(chargeResponse.getBody().cost()).isEqualTo(0); // 충전 시 cost는 0

        // when: 포인트 조회
        ResponseEntity<UserPoint> getResponse = restTemplate.getForEntity(
                "/point/{id}",
                UserPoint.class,
                userId
        );

        // then: 조회 결과가 충전한 금액과 일치
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().id()).isEqualTo(userId);
        assertThat(getResponse.getBody().point()).isEqualTo(chargeAmount);
    }

    @Test
    @DisplayName("통합 테스트: 포인트 충전 후 내역 조회가 정상적으로 동작한다")
    void chargeAndGetHistory_integration() {
        // given: 유저 ID와 충전 금액
        long userId = 101L;
        long chargeAmount = 10000L;

        // when: 포인트 충전
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Long> chargeRequest = new HttpEntity<>(chargeAmount, headers);

        ResponseEntity<UserPoint> chargeResponse = restTemplate.exchange(
                "/point/{id}/charge",
                HttpMethod.PATCH,
                chargeRequest,
                UserPoint.class,
                userId
        );

        // then: 충전 성공
        assertThat(chargeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // when: 포인트 내역 조회
        ResponseEntity<PointHistory[]> historyResponse = restTemplate.getForEntity(
                "/point/{id}/histories",
                PointHistory[].class,
                userId
        );

        // then: 내역이 기록되어 있음
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(historyResponse.getBody()).isNotNull();
        assertThat(historyResponse.getBody()).hasSize(1);
        assertThat(historyResponse.getBody()[0].userId()).isEqualTo(userId);
        assertThat(historyResponse.getBody()[0].amount()).isEqualTo(chargeAmount);
        assertThat(historyResponse.getBody()[0].type()).isEqualTo(TransactionType.CHARGE);
    }

    @Test
    @DisplayName("통합 테스트: 포인트 충전, 사용 후 전체 내역 조회")
    void fullScenario_chargeMultipleTimes() {
        // given: 유저 ID
        long userId = 103L;
        long firstCharge = 10000L;
        long secondCharge = 5000L;

        // when: 첫 번째 충전
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Long> firstRequest = new HttpEntity<>(firstCharge, headers);

        restTemplate.exchange(
                "/point/{id}/charge",
                HttpMethod.PATCH,
                firstRequest,
                UserPoint.class,
                userId
        );

        // when: 두 번째 충전
        HttpEntity<Long> secondRequest = new HttpEntity<>(secondCharge, headers);

        restTemplate.exchange(
                "/point/{id}/charge",
                HttpMethod.PATCH,
                secondRequest,
                UserPoint.class,
                userId
        );

        // when: 포인트 조회
        ResponseEntity<UserPoint> pointResponse = restTemplate.getForEntity(
                "/point/{id}",
                UserPoint.class,
                userId
        );

        // then: 총 포인트가 합산되어 있음
        assertThat(pointResponse.getBody()).isNotNull();
        assertThat(pointResponse.getBody().point()).isEqualTo(firstCharge + secondCharge);

        // when: 전체 내역 조회
        ResponseEntity<PointHistory[]> historyResponse = restTemplate.getForEntity(
                "/point/{id}/histories",
                PointHistory[].class,
                userId
        );

        // then: 2개의 충전 내역이 있음
        assertThat(historyResponse.getBody()).isNotNull();
        assertThat(historyResponse.getBody()).hasSize(2);
        assertThat(historyResponse.getBody()[0].type()).isEqualTo(TransactionType.CHARGE);
        assertThat(historyResponse.getBody()[1].type()).isEqualTo(TransactionType.CHARGE);
    }

    @Test
    @DisplayName("통합 테스트: 존재하지 않는 유저의 포인트 조회 시 빈 포인트 반환")
    void getUserPoint_nonExistentUser_returnsEmptyPoint() {
        // given: 존재하지 않는 유저 ID
        long nonExistentUserId = 999L;

        // when: 포인트 조회
        ResponseEntity<UserPoint> response = restTemplate.getForEntity(
                "/point/{id}",
                UserPoint.class,
                nonExistentUserId
        );

        // then: 빈 포인트 반환 (point = 0)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(nonExistentUserId);
        assertThat(response.getBody().point()).isEqualTo(0);
        assertThat(response.getBody().cost()).isEqualTo(0);
    }

    @Test
    @DisplayName("통합 테스트: 정책1 - 100만원 이상 충전 시 예외 발생")
    void charge_overOneMillionWon_throwsException() {
        // given: 유저 ID와 100만원 이상의 충전 금액
        long userId = 200L;
        long chargeAmount = 1000000L;

        // when: 포인트 충전 시도
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Long> chargeRequest = new HttpEntity<>(chargeAmount, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/point/{id}/charge",
                HttpMethod.PATCH,
                chargeRequest,
                String.class,
                userId
        );

        // then: 에러 응답 (400 or 500)
        assertThat(response.getStatusCode().isError()).isTrue();
    }

    @Test
    @DisplayName("통합 테스트: 포인트 충전 후 사용이 정상적으로 동작한다")
    void chargeAndUsePoint_integration() {
        // given: 유저가 20000원을 충전한 상태
        long userId = 201L;
        long chargeAmount = 20000L;

        // 먼저 포인트 충전
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Long> chargeRequest = new HttpEntity<>(chargeAmount, headers);

        restTemplate.exchange(
                "/point/{id}/charge",
                HttpMethod.PATCH,
                chargeRequest,
                UserPoint.class,
                userId
        );

        // when: 5000원 포인트를 사용 (결제 금액 20000원)
        long useAmount = 5000L;
        long cost = 20000L;
        String useRequestBody = String.format(
                "{\"id\": %d, \"point\": %d, \"updateMillis\": %d, \"cost\": %d}",
                userId, useAmount, System.currentTimeMillis(), cost
        );
        HttpEntity<String> useRequest = new HttpEntity<>(useRequestBody, headers);

        ResponseEntity<UserPoint> useResponse = restTemplate.exchange(
                "/point/{id}/use",
                HttpMethod.PATCH,
                useRequest,
                UserPoint.class,
                userId
        );

        // then: 사용 성공 및 잔액 확인
        assertThat(useResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(useResponse.getBody()).isNotNull();
        assertThat(useResponse.getBody().id()).isEqualTo(userId);
        assertThat(useResponse.getBody().point()).isEqualTo(chargeAmount - useAmount); // 15000원
        assertThat(useResponse.getBody().cost()).isEqualTo(cost);

        // when: 포인트 내역 조회
        ResponseEntity<PointHistory[]> historyResponse = restTemplate.getForEntity(
                "/point/{id}/histories",
                PointHistory[].class,
                userId
        );

        // then: 충전 1회 + 사용 1회 = 총 2개의 내역
        assertThat(historyResponse.getBody()).isNotNull();
        assertThat(historyResponse.getBody()).hasSize(2);
        assertThat(historyResponse.getBody()[0].type()).isEqualTo(TransactionType.CHARGE);
        assertThat(historyResponse.getBody()[1].type()).isEqualTo(TransactionType.USE);
    }

    @Test
    @DisplayName("통합 테스트: 정책2 - 10000원 이하 가격에 포인트 사용 불가")
    void use_costUnder10000_throwsException() {
        // given: 유저가 20000원을 충전한 상태
        long userId = 202L;
        long chargeAmount = 20000L;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Long> chargeRequest = new HttpEntity<>(chargeAmount, headers);

        restTemplate.exchange(
                "/point/{id}/charge",
                HttpMethod.PATCH,
                chargeRequest,
                UserPoint.class,
                userId
        );

        // when: 10000원 이하의 가격(5000원)에 포인트 사용 시도
        long useAmount = 1000L;
        long cost = 5000L; // 10000원 이하
        String useRequestBody = String.format(
                "{\"id\": %d, \"point\": %d, \"updateMillis\": %d, \"cost\": %d}",
                userId, useAmount, System.currentTimeMillis(), cost
        );
        HttpEntity<String> useRequest = new HttpEntity<>(useRequestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/point/{id}/use",
                HttpMethod.PATCH,
                useRequest,
                String.class,
                userId
        );

        // then: 에러 응답
        assertThat(response.getStatusCode().isError()).isTrue();
    }

    @Test
    @DisplayName("통합 테스트: 정책3 - 결제 금액의 50% 초과 사용 불가")
    void use_amountExceeds50Percent_throwsException() {
        // given: 유저가 30000원을 충전한 상태
        long userId = 203L;
        long chargeAmount = 30000L;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Long> chargeRequest = new HttpEntity<>(chargeAmount, headers);

        restTemplate.exchange(
                "/point/{id}/charge",
                HttpMethod.PATCH,
                chargeRequest,
                UserPoint.class,
                userId
        );

        // when: 결제 금액 20000원에 대해 15000원 사용 시도 (75% > 50%)
        long useAmount = 15000L;
        long cost = 20000L;
        String useRequestBody = String.format(
                "{\"id\": %d, \"point\": %d, \"updateMillis\": %d, \"cost\": %d}",
                userId, useAmount, System.currentTimeMillis(), cost
        );
        HttpEntity<String> useRequest = new HttpEntity<>(useRequestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/point/{id}/use",
                HttpMethod.PATCH,
                useRequest,
                String.class,
                userId
        );

        // then: 에러 응답
        assertThat(response.getStatusCode().isError()).isTrue();
    }

    @Test
    @DisplayName("통합 테스트: 잔고 부족 시 사용 불가")
    void use_insufficientBalance_throwsException() {
        // given: 유저가 5000원을 충전한 상태
        long userId = 204L;
        long chargeAmount = 5000L;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Long> chargeRequest = new HttpEntity<>(chargeAmount, headers);

        restTemplate.exchange(
                "/point/{id}/charge",
                HttpMethod.PATCH,
                chargeRequest,
                UserPoint.class,
                userId
        );

        // when: 10000원 사용 시도 (잔고보다 많음)
        long useAmount = 10000L;
        long cost = 30000L;
        String useRequestBody = String.format(
                "{\"id\": %d, \"point\": %d, \"updateMillis\": %d, \"cost\": %d}",
                userId, useAmount, System.currentTimeMillis(), cost
        );
        HttpEntity<String> useRequest = new HttpEntity<>(useRequestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/point/{id}/use",
                HttpMethod.PATCH,
                useRequest,
                String.class,
                userId
        );

        // then: 에러 응답
        assertThat(response.getStatusCode().isError()).isTrue();
    }
}