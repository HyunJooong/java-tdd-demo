package io.hhplus.tdd.integration;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class PointIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        // Apache HttpClient를 사용하여 PATCH 메서드 지원
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    }

    @Test
    @DisplayName("통합 테스트: 포인트를 충전한다")
    void chargePoint_integration() {
        // given: 유저 ID와 충전 금액
        long userId = 1L;
        long chargeAmount = 5000L;

        // when: 포인트 충전 요청
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(String.valueOf(chargeAmount), headers);

        ResponseEntity<UserPoint> response = restTemplate.exchange(
                "/point/{id}/charge",
                HttpMethod.PATCH,
                request,
                UserPoint.class,
                userId
        );

        // then: 충전 성공
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(userId);
        assertThat(response.getBody().point()).isEqualTo(chargeAmount);
        assertThat(response.getBody().cost()).isEqualTo(0); // 충전 시 cost는 0
    }

    @Test
    @DisplayName("통합 테스트: 특정 유저의 포인트를 조회한다")
    void getUserPoint_integration() {
        // given: 유저 ID
        long userId = 2L;

        // when: 포인트 조회 요청
        ResponseEntity<UserPoint> response = restTemplate.getForEntity(
                "/point/{id}",
                UserPoint.class,
                userId
        );

        // then: 조회 성공
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(userId);
        assertThat(response.getBody().point()).isGreaterThanOrEqualTo(0); // 포인트는 0 이상
    }

    @Test
    @DisplayName("통합 테스트: 포인트 충전 후 조회가 정상적으로 동작한다")
    void chargeAndGetUserPoint_integration() {
        // given: 유저 ID와 충전 금액
        long userId = 3L;
        long chargeAmount = 10000L;

        // when: 포인트 충전
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> chargeRequest = new HttpEntity<>(String.valueOf(chargeAmount), headers);

        restTemplate.exchange(
                "/point/{id}/charge",
                HttpMethod.PATCH,
                chargeRequest,
                UserPoint.class,
                userId
        );

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
    @DisplayName("통합 테스트: 포인트를 사용한다")
    void usePoint_integration() {
        // given: 먼저 포인트를 충전
        long userId = 4L;
        long chargeAmount = 10000L;  // 포인트 충전

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> chargeRequest = new HttpEntity<>(String.valueOf(chargeAmount), headers);

        restTemplate.exchange(
                "/point/{id}/charge",
                HttpMethod.PATCH,
                chargeRequest,
                UserPoint.class,
                userId
        );

        // when: 포인트 사용
        long useAmount = 5000L;
        long cost = 20000L;

        String useRequestBody = String.format(
                "{\"id\": %d, \"point\": %d, \"updateMillis\": %d, \"cost\": %d}",
                userId, useAmount, System.currentTimeMillis(), cost
        );
        HttpEntity<String> useRequest = new HttpEntity<>(useRequestBody, headers);

        ResponseEntity<UserPoint> response = restTemplate.exchange(
                "/point/{id}/use",
                HttpMethod.PATCH,
                useRequest,
                UserPoint.class,
                userId
        );

        // then: 사용 성공 및 남은 포인트 확인
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(userId);
        assertThat(response.getBody().point()).isEqualTo(chargeAmount - useAmount);  // 5000 남음
        assertThat(response.getBody().cost()).isEqualTo(cost);
    }

    @Test
    @DisplayName("통합 테스트: 포인트 충전/이용 내역을 조회한다")
    void getPointHistory_integration() {
        // given: 유저 ID
        long userId = 5L;

        // when: 내역 조회 요청 (초기 상태)
        ResponseEntity<List<PointHistory>> initialResponse = restTemplate.exchange(
                "/point/{id}/histories",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<PointHistory>>() {},
                userId
        );

        // then: 초기 내역은 비어있음 (0개)
        assertThat(initialResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(initialResponse.getBody()).isNotNull();
        assertThat(initialResponse.getBody().size()).isEqualTo(0);

        // given: 포인트 충전
        long chargeAmount = 10000L;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> chargeRequest = new HttpEntity<>(String.valueOf(chargeAmount), headers);

        restTemplate.exchange(
                "/point/{id}/charge",
                HttpMethod.PATCH,
                chargeRequest,
                UserPoint.class,
                userId
        );

        // when: 충전 후 내역 조회
        ResponseEntity<List<PointHistory>> afterChargeResponse = restTemplate.exchange(
                "/point/{id}/histories",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<PointHistory>>() {},
                userId
        );

        // then: 충전 내역이 추가됨 (1개)
        assertThat(afterChargeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(afterChargeResponse.getBody()).isNotNull();
        assertThat(afterChargeResponse.getBody().size()).isEqualTo(1);

        PointHistory chargeHistory = afterChargeResponse.getBody().get(0);
        assertThat(chargeHistory.userId()).isEqualTo(userId);
        assertThat(chargeHistory.amount()).isEqualTo(chargeAmount);
        assertThat(chargeHistory.type()).isEqualTo(TransactionType.CHARGE);

        // given: 포인트 사용 (usePoint_integration과 동일한 패턴)
        long useAmount = 5000L;
        long cost = 20000L;  // 10000원 초과로 설정
        String useRequestBody = String.format(
                "{\"id\": %d, \"point\": %d, \"updateMillis\": %d, \"cost\": %d}",
                userId, useAmount, System.currentTimeMillis(), cost
        );
        HttpEntity<String> useRequest = new HttpEntity<>(useRequestBody, headers);

        restTemplate.exchange(
                "/point/{id}/use",
                HttpMethod.PATCH,
                useRequest,
                UserPoint.class,
                userId
        );

        // when: 사용 후 내역 조회
        ResponseEntity<List<PointHistory>> afterUseResponse = restTemplate.exchange(
                "/point/{id}/histories",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<PointHistory>>() {},
                userId
        );

        // then: 사용 내역이 추가됨 (2개)
        assertThat(afterUseResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(afterUseResponse.getBody()).isNotNull();
        assertThat(afterUseResponse.getBody().size()).isEqualTo(2);

        PointHistory useHistory = afterUseResponse.getBody().get(1);
        assertThat(useHistory.userId()).isEqualTo(userId);
        assertThat(useHistory.amount()).isEqualTo(useAmount);
        assertThat(useHistory.type()).isEqualTo(TransactionType.USE);
    }

}
