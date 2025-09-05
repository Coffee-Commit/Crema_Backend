package coffeandcommit.crema.domain.reservation.dto.request;

import coffeandcommit.crema.domain.reservation.enums.Status;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservationDecisionRequestDTO {

    @NotNull(message = "상태 값은 필수입니다.")
    private Status status; // CONFIRMED or CANCELLED
}
