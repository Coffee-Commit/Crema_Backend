package coffeandcommit.crema.domain.review.dto.response;

import coffeandcommit.crema.domain.guide.enums.TimeType;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationInfo {

    private LocalDateTime matchingDateTime; // 확정된 커피챗 일시 (날짜+시간)
    private TimeType timeUnit;

    public static ReservationInfo from(Reservation reservation) {
        return ReservationInfo.builder()
                .matchingDateTime(reservation.getMatchingTime())
                .timeUnit(reservation.getTimeUnit().getTimeType())
                .build();
    }
}
