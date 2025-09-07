package coffeandcommit.crema.domain.guide.dto.response;

import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideThisWeekCoffeeChatResponseDTO {

    private Long reservationId;
    private MemberInfo member;
    private String createdAt;
    private String preferredDateOnly;
    private String preferredDayOfWeek;
    private String preferredTimeRange;
    private Status status;

    public static GuideThisWeekCoffeeChatResponseDTO from(Reservation reservation, MemberInfo memberInfo,
                                                          String preferredDateOnly, String preferredDayOfWeek,
                                                          String preferredTimeRange) {
        return GuideThisWeekCoffeeChatResponseDTO.builder()
                .reservationId(reservation.getId())
                .member(memberInfo)
                .createdAt(reservation.getCreatedAt() != null ? reservation.getCreatedAt().toString() : null)
                .preferredDateOnly(preferredDateOnly)
                .preferredDayOfWeek(preferredDayOfWeek)
                .preferredTimeRange(preferredTimeRange)
                .status(reservation.getStatus())
                .build();
    }
}
