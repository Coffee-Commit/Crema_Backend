package coffeandcommit.crema.domain.reservation.dto.response;

import coffeandcommit.crema.domain.reservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservationCompletionResponseDTO {

    private Long reservationId;        // 예약 ID
    private String title;              // 신청한 커피챗 제목/이름
    private String preferredDateOnly;      // 희망 날짜 (yyyy-MM-dd)
    private String preferredDayOfWeek; // 희망 요일 (예: 월, 화, 수)
    private String preferredTimeRange;      // 희망 시간대 (예: "19:00~19:30")
    private Integer price;             // 결제 금액 (원 단위)
    private String status;             // 예약 상태 (예: COMPLETED, CANCELED 등)

    public static ReservationCompletionResponseDTO from(Reservation reservation,
                                                        String preferredDateOnly,
                                                        String preferredDayOfWeek,
                                                        String preferredTimeRange,
                                                        Integer price) {
        return ReservationCompletionResponseDTO.builder()
                .reservationId(reservation.getId())
                .title(reservation.getGuide().getTitle())
                .preferredDateOnly(preferredDateOnly)
                .preferredDayOfWeek(preferredDayOfWeek)
                .preferredTimeRange(preferredTimeRange)
                .price(price)
                .status(reservation.getStatus().name())
                .build();
    }
}
