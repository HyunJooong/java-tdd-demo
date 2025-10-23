package io.hhplus.tdd.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PointServiceConcurrencyTest {

    private PointService pointService;
    private UserPointTable userPointTable;
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        userPointTable = new UserPointTable();
        pointHistoryTable = new PointHistoryTable();
        pointService = new PointServiceImpl(userPointTable, pointHistoryTable);
    }

    @Test
    @DisplayName("동시성 테스트: 동일한 사용자가 동시에 포인트 충전")
    void concurrentCharge_sameUser() throws InterruptedException {
        // given: 유저 1L이 0 포인트를 가지고 있음
        long userId = 1L;
        int threadCount = 10;
        long chargeAmount = 1000L;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when: 10개의 스레드가 동시에 1000원씩 충전
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.charge(userId, chargeAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드가 완료될 때까지 대기
        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: 최종 포인트는 10000원이어야 함 (1000 * 10)
        UserPoint result = pointService.getUserPoint(userId);
        assertThat(result.point()).isEqualTo(chargeAmount * threadCount);
    }

    @Test
    @DisplayName("동시성 테스트: 동일한 사용자가 동시에 포인트 사용")
    void concurrentUse_sameUser() throws InterruptedException {
        // given: 유저 2L이 100000 포인트를 가지고 있음
        long userId = 2L;
        long initialPoint = 100000L;
        userPointTable.insertOrUpdate(userId, initialPoint, 0);

        int threadCount = 10;
        long useAmount = 5000L;
        long cost = 20000L;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when: 10개의 스레드가 동시에 5000원씩 사용
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.use(userId, useAmount, cost);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드가 완료될 때까지 대기
        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: 최종 포인트는 50000원이어야 함 (100000 - 5000 * 10)
        UserPoint result = pointService.getUserPoint(userId);
        assertThat(result.point()).isEqualTo(initialPoint - (useAmount * threadCount));
    }

    @Test
    @DisplayName("동시성 테스트: 동일한 사용자가 동시에 충전과 사용")
    void concurrentChargeAndUse_sameUser() throws InterruptedException {
        // given: 유저 3L이 50000 포인트를 가지고 있음
        long userId = 3L;
        long initialPoint = 50000L;
        userPointTable.insertOrUpdate(userId, initialPoint, 0);

        int threadCount = 20; // 10개 충전, 10개 사용
        long chargeAmount = 1000L;
        long useAmount = 500L;
        long cost = 20000L;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when: 10개의 스레드는 충전, 10개의 스레드는 사용
        for (int i = 0; i < threadCount / 2; i++) {
            // 충전 스레드
            executorService.submit(() -> {
                try {
                    pointService.charge(userId, chargeAmount);
                } finally {
                    latch.countDown();
                }
            });

            // 사용 스레드
            executorService.submit(() -> {
                try {
                    pointService.use(userId, useAmount, cost);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드가 완료될 때까지 대기
        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: 최종 포인트 = 50000 + (1000 * 10) - (500 * 10) = 55000
        UserPoint result = pointService.getUserPoint(userId);
        assertThat(result.point()).isEqualTo(initialPoint + (chargeAmount * 10) - (useAmount * 10));
    }
}