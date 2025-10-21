package io.hhplus.tdd.service;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PointServiceTest {

    private PointService pointService;
    private UserPointTable userPointTable;

    @BeforeEach
    void setUp() {
        userPointTable = new UserPointTable();
        pointService = new PointServiceImpl(userPointTable);
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
}
