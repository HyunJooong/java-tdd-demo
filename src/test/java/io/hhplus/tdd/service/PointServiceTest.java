package io.hhplus.tdd.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointServiceTest {

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
    @DisplayName("특정 유저의 포인트를 조회한다")
    void getUserPoint() {
        // given: 유저 1L의 포인트가 1000L로 저장되어 있음
        long userId = 1L;
        long expectedPoint = 1000L;
        userPointTable.insertOrUpdate(userId, expectedPoint);

        // when: 유저의 포인트를 조회
        UserPoint result = pointService.getUserPoint(userId);

        // then: 저장된 포인트가 조회됨
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(expectedPoint);
    }

    @Test
    @DisplayName("특정 유저의 여러 포인트 내역을 조회한다 - GREEN & Refactor")
    void getPointHistory_withMultipleHistories_success() {
        // given: 유저 1L의 여러 포인트 내역이 저장되어 있음
        long userId = 1L;
        long chargeAmount = 1000L;
        long useAmount = 500L;
        long currentTime = System.currentTimeMillis();
        pointHistoryTable.insert(userId, chargeAmount, TransactionType.CHARGE, currentTime);
        pointHistoryTable.insert(userId, useAmount, TransactionType.USE, currentTime);

        // when: 유저의 포인트 내역을 조회
        List<PointHistory> result = pointService.getPointHistory(userId);

        // then: 저장된 모든 내역이 조회됨
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).userId()).isEqualTo(userId);
        assertThat(result.get(0).amount()).isEqualTo(chargeAmount);
        assertThat(result.get(0).type()).isEqualTo(TransactionType.CHARGE);
        assertThat(result.get(1).userId()).isEqualTo(userId);
        assertThat(result.get(1).amount()).isEqualTo(useAmount);
        assertThat(result.get(1).type()).isEqualTo(TransactionType.USE);
    }
    
    @Test
    @DisplayName("포인트가 없는 새로운 유저의 포인트를 충전한다")
    void chargeUserPoint_newUser_success() {
        // given: 포인트가 없는 새로운 유저
        long userId = 2L;
        long chargeAmount = 1000L;

        // when: 1000L 포인트를 충전
        UserPoint result = pointService.charge(userId, chargeAmount);

        // then: 충전된 포인트가 반환됨
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(chargeAmount);

        // then: 충전 내역이 기록됨
        List<PointHistory> histories = pointService.getPointHistory(userId);
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).userId()).isEqualTo(userId);
        assertThat(histories.get(0).amount()).isEqualTo(chargeAmount);
        assertThat(histories.get(0).type()).isEqualTo(TransactionType.CHARGE);
    }

    @Test
    @DisplayName("기존 포인트가 있는 유저의 포인트를 추가 충전한다")
    void chargeUserPoint_existingUser_success() {
        // given: 유저 3L의 초기 포인트가 500L
        long userId = 3L;
        long initialPoint = 500L;
        long chargeAmount = 1000L;
        userPointTable.insertOrUpdate(userId, initialPoint);

        // when: 1000L 포인트를 추가 충전
        UserPoint result = pointService.charge(userId, chargeAmount);

        // then: 충전 후 포인트가 1500L이 됨
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(initialPoint + chargeAmount);

        // then: 충전 내역이 기록됨
        List<PointHistory> histories = pointService.getPointHistory(userId);
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).userId()).isEqualTo(userId);
        assertThat(histories.get(0).amount()).isEqualTo(chargeAmount);
        assertThat(histories.get(0).type()).isEqualTo(TransactionType.CHARGE);
    }

}
