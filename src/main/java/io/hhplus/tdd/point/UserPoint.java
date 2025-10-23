package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis,
        long cost
) {

    public static UserPoint empty(long id) {

        return new UserPoint(id, 0, System.currentTimeMillis(),0);
    }
}
