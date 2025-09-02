package coffeandcommit.crema.domain.guide.dto.request;

import coffeandcommit.crema.domain.guide.enums.DayType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ScheduleRequestDTO {

    @NotNull(message = "요일은 필수 값입니다.")
    private DayType day;

    @NotEmpty(message = "시간 구간은 최소 1개 이상이어야 합니다.")
    private List<TimeSlotRequestDTO> timeSlots;
}
