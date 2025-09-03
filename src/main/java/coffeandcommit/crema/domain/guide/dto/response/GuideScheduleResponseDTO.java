package coffeandcommit.crema.domain.guide.dto.response;

import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.GuideSchedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideScheduleResponseDTO {

    private Long guideId; // 가이드 ID
    private List<ScheduleResponseDTO> schedules;

    public static GuideScheduleResponseDTO from(Guide guide, List<GuideSchedule> guideSchedules) {
        return GuideScheduleResponseDTO.builder()
                .guideId(guide.getId())
                .schedules(guideSchedules.stream()
                        .sorted(Comparator.comparing(guideSchedule -> guideSchedule.getDayOfWeek().getOrder()))
                        .map(ScheduleResponseDTO::from)
                        .toList())
                .build();
    }

}
