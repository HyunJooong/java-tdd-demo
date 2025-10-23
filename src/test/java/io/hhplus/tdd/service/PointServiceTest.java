package io.hhplus.tdd.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.InsufficientPointException;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        userPointTable.insertOrUpdate(userId, expectedPoint,0);

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
        userPointTable.insertOrUpdate(userId, initialPoint,0);

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

    @Test
    @DisplayName("유저의 포인트를 사용한다")
    void useUserPoint_success() {
        // given: 유저 4L의 초기 포인트가 1000L
        long userId = 4L;
        long initialPoint = 1000L;
        long useAmount = 300L;
        long cost = 10000L;
        userPointTable.insertOrUpdate(userId, initialPoint, cost);

        // when: 300L 포인트를 사용
        UserPoint result = pointService.use(userId, useAmount, cost);

        // then: 사용 후 포인트가 700L이 됨
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(initialPoint - useAmount);

        // then: 사용 내역이 기록됨
        List<PointHistory> histories = pointService.getPointHistory(userId);
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).userId()).isEqualTo(userId);
        assertThat(histories.get(0).amount()).isEqualTo(useAmount);
        assertThat(histories.get(0).type()).isEqualTo(TransactionType.USE);
    }

    @Test
    @DisplayName("현재 포인트(currentPoint)가 사용할 금액(amount)보다 적으면 포인트가 부족해 실패한다")
    void useUserPoint_whenCurrentPointLessThanAmount_throwsInsufficientPointException() {
        // given: currentPoint(5000L) < amount(10000L) 상황 설정
        long userId = 1L;
        long currentPoint = 5000L;
        long amount = 10000L;
        long cost = 20000L;

        userPointTable.insertOrUpdate(userId, currentPoint, cost);

        // when: amount가 currentPoint보다 큰 경우 포인트 사용 시도
        InsufficientPointException exception = assertThrows(
                InsufficientPointException.class,
                () -> pointService.use(userId, amount, cost)
        );

        // then: "포인트가 부족합니다." 메시지 확인
        assertEquals("포인트가 부족합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("정책3: 포인트는 결제 금액의 최대 50%까지만 사용할 수 있다")
    void useUserPoint_whenAmountExceedsMaxUsablePoint_throwsInsufficientPointException() {
        // given: cost = 20000원, maxUsablePoint = 10000원, amount = 15000원 (50% 초과)
        long userId = 6L;
        long currentPoint = 20000L;
        long cost = 20000L;
        long amount = 15000L; // cost의 75% (50% 초과)
        userPointTable.insertOrUpdate(userId, currentPoint,cost);

        // when: amount가 maxUsablePoint(cost / 2)를 초과하는 경우 포인트 사용 시도
        InsufficientPointException exception = assertThrows(
                InsufficientPointException.class,
                () -> pointService.use(userId, amount, cost)
        );

        // then: "포인트는 결제 금액의 최대 50%까지만 사용할 수 있습니다." 메시지 확인
        assertEquals("포인트는 결제 금액의 최대 50%까지만 사용할 수 있습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("정책3: 포인트를 결제 금액의 정확히 50%로 사용할 수 있다")
    void useUserPoint_whenAmountIsExactly50Percent_success() {
        // given: cost = 20000원, maxUsablePoint = 10000원, amount = 10000원 (정확히 50%)
        long userId = 7L;
        long currentPoint = 20000L;
        long cost = 20000L;
        long amount = 10000L; // cost의 정확히 50%
        userPointTable.insertOrUpdate(userId, currentPoint,cost);

        // when: amount가 maxUsablePoint(cost / 2)와 같은 경우 포인트 사용
        UserPoint result = pointService.use(userId, amount, cost);

        // then: 포인트 사용 성공 및 잔액 확인
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(currentPoint - amount);

        // then: 사용 내역이 기록됨
        List<PointHistory> histories = pointService.getPointHistory(userId);
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).amount()).isEqualTo(amount);
        assertThat(histories.get(0).type()).isEqualTo(TransactionType.USE);
    }
}
