package coffeandcommit.crema.domain.guide.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GuideScheduleRequestDTO {

    @NotEmpty(message = "스케줄은 최소 1개 이상이어야 합니다.")
    private List<ScheduleRequestDTO> schedules;
}
