package io.hhplus.tdd.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.InsufficientPointException;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    @Override
    public UserPoint getUserPoint(long id) {
        return userPointTable.selectById(id);
    }

    @Override
    public List<PointHistory> getPointHistory(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    @Override
    public UserPoint charge(long id, long amount) {
        // 현재 포인트 조회
        UserPoint currentUserPoint = getUserPoint(id);
        long currentPoint = (currentUserPoint == null || currentUserPoint.point() == 0) ? 0 : currentUserPoint.point();


        //정책: 포인트 충전은 100만원 이상 할 수 없다.
        if(amount >= 1000000){
            throw new InsufficientPointException("포인트를 100만원 이상 충전할 수 없습니다.");
        }
        // 포인트 충전
        long newPoint = currentPoint + amount;
        UserPoint updatedUserPoint = userPointTable.insertOrUpdate(id, newPoint);

        // 충전 내역 기록
        long updateMillis = System.currentTimeMillis();
        pointHistoryTable.insert(id, amount, TransactionType.CHARGE, updateMillis);

        return updatedUserPoint;
    }

    @Override
    public UserPoint use(long id, long amount, long cost) {
        // 현재 포인트 조회
        UserPoint currentUserPoint = getUserPoint(id);
        long currentPoint = (currentUserPoint == null || currentUserPoint.point() == 0) ? 0 : currentUserPoint.point();

        // 포인트 부족 예외 처리
        if (currentPoint <= 0 || currentPoint < amount) {
            throw new InsufficientPointException("포인트가 부족합니다.");
        }

        // 정책: 포인트는 10000원 이하의 가격에는 사용할 수 없다.
        if (cost <= 10000) {
            throw new InsufficientPointException("10000원 이하의 가격에는 포인트를 사용할 수 없습니다.");
        }

        // 포인트 사용
        long balance = currentPoint - amount;
        UserPoint updatedUserPoint = userPointTable.insertOrUpdate(id, balance);

        // 사용 내역 기록
        long updateMillis = System.currentTimeMillis();
        pointHistoryTable.insert(id, amount, TransactionType.USE, updateMillis);

        return updatedUserPoint;
    }
}
