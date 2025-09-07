package coffeandcommit.crema.domain.guide.dto.response;

import coffeandcommit.crema.domain.reservation.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuidePendingReservationResponseDTO {

    private Long reservationId;       // 예약 ID (PK)
    private MemberInfo member;        // 멘티 정보
    private String createdAt;         // 신청 완료된 시각 (ISO String)
    private String preferredDateOnly;     // 희망 날짜 (yyyy-MM-dd)
    private String preferredDayOfWeek;  // 요일 (예: 월요일)
    private String preferredTimeRange;     // 희망 시간 범위 (HH:mm~HH:mm)
    private Status status;            // 예약 상태 (PENDING/CONFIRMED/COMPLETED/CANCELLED)

    public static GuidePendingReservationResponseDTO from(
            Long reservationId,
            MemberInfo member,
            String createdAt,
            String preferredDateOnly,
            String preferredDayOfWeek,
            String preferredTimeRange,
            Status status
    ) {
        return GuidePendingReservationResponseDTO.builder()
                .reservationId(reservationId)
                .member(member)
                .createdAt(createdAt)
                .preferredDateOnly(preferredDateOnly)
                .preferredDayOfWeek(preferredDayOfWeek)
                .preferredTimeRange(preferredTimeRange)
                .status(status)
                .build();
    }
}
