package coffeandcommit.crema.domain.reservation.dto.request;

import coffeandcommit.crema.domain.guide.enums.TimeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservationRequestDTO {

    @NotNull(message = "가이드 ID는 필수입니다.")
    private Long guideId;

    @NotNull(message = "시간 단위는 필수입니다.")
    private TimeType timeUnit; // Enum (THIRTY_MINUTES, SIXTY_MINUTES)

    @Valid
    @NotNull(message = "사전자료는 필수입니다.")
    private SurveyRequestDTO survey; // 예약 시 함께 등록되는 사전자료
}
