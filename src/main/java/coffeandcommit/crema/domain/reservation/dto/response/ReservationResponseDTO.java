package coffeandcommit.crema.domain.reservation.dto.response;

import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.global.storage.StorageService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservationResponseDTO {

    private Long reservationId;
    private Long guideId;
    private String status; // Enum -> String 직렬화
    private String timeUnit; // Enum -> String 직렬화
    private SurveyResponseDTO survey;
    private LocalDateTime createdAt;

    public static ReservationResponseDTO from(Reservation reservation, StorageService storageService) {
        return ReservationResponseDTO.builder()
                .reservationId(reservation.getId())
                .guideId(reservation.getGuide().getId())
                .status(reservation.getStatus().name())
                .timeUnit(reservation.getTimeUnit().getTimeType().name())
                .survey(SurveyResponseDTO.from(reservation.getSurvey(), storageService))
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}
