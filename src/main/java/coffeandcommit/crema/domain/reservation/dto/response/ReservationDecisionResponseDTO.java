package coffeandcommit.crema.domain.reservation.dto.response;

import coffeandcommit.crema.domain.reservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservationDecisionResponseDTO {

    private Long reservationId;
    private Long guideId;
    private String status;   // Enum → String
    private String timeUnit; // Enum → String
    private LocalDateTime updatedAt;

    public static ReservationDecisionResponseDTO from(Reservation reservation) {
        return ReservationDecisionResponseDTO.builder()
                .reservationId(reservation.getId())
                .guideId(reservation.getGuide().getId())
                .status(reservation.getStatus().name())
                .timeUnit(reservation.getTimeUnit().getTimeType().name())
                .updatedAt(reservation.getModifiedAt())
                .build();
    }

}
