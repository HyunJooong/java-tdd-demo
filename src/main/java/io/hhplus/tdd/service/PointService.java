package io.hhplus.tdd.service;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.UserPoint;

import java.util.List;

public interface PointService {

    /**
     * 특정 유저 포인트 조회
     * @param id 사용자 ID 값
     * @return
     */
    UserPoint getUserPoint(long id);

    /**
     * 사용자의 포인트 이용 및 충전 기록 조회
     * @param userId 사용자 고유값
     * @return
     */
    List<PointHistory> getPointHistory(long userId);

    /**
     * 사용자 포인트 충전하는 기능
     * @param id 사용자 ID
     * @param amount 충전 포인트
     * @return
     */
    UserPoint charge(long id, long amount);

    /**
     * 사용자가 포인트를 사용한다.
     * @param id 사용자 ID
     * @param amount 사용 금액
     * @return
     */
    UserPoint use(long id, long amount);

}
