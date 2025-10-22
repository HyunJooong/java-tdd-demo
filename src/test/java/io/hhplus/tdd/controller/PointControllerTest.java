package io.hhplus.tdd.controller;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointController.class)
public class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointService pointService; //가짜 객체 사용

    @Test
    @DisplayName("특정 유저의 포인트를 조회한다")
    void getUserPoint() throws Exception {
        // given - Mock을 사용하여 PointService의 동작을 시뮬레이션
        long userId = 1L;
        long expectedPoint = 1000L;
        UserPoint userPoint = new UserPoint(userId, expectedPoint, System.currentTimeMillis());
        given(pointService.getUserPoint(anyLong())).willReturn(userPoint);

        // when - HTTP GET 요청 수행
        var result = mockMvc.perform(get("/point/{id}", userId));

        // then - 응답 검증
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(expectedPoint));
    }

    @Test
    @DisplayName("특정 유저의 포인트 충전/이용 내역을 조회한다")
    void getUserPointHistories() throws Exception {
        // given
        long userId = 1L;
        long amount = 1000L;

        // when
        ResultActions result = mockMvc.perform(
                get("/point/{id}/histories", userId)
        );

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))  // 1개를 기대하지만 0개 반환 -> FAIL
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].amount").value(amount))
                .andExpect(jsonPath("$[0].type").value("CHARGE"));
    }
}
