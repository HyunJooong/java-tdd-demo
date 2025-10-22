package io.hhplus.tdd.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
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
    @DisplayName("히스토리가 없는 유저의 포인트 내역 조회 시 예외가 발생한다 - RED")
    void getPointHistory_noHistory_throwsException() {
        // given: 히스토리가 전혀 없는 유저 ID
        long userId = 999L;

        // when: 포인트 내역 조회
        List<PointHistory> result = pointService.getPointHistory(userId);

        // then: 빈 리스트가 아닌 예외가 발생해야 함
        // 현재 구현은 빈 리스트를 반환하므로 이 테스트는 실패함 (RED)
        assertThat(result).isNotEmpty();
    }
}
