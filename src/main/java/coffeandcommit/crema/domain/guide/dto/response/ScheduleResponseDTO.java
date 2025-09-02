package coffeandcommit.crema.domain.guide.dto.response;

import coffeandcommit.crema.domain.guide.entity.GuideSchedule;
import coffeandcommit.crema.domain.guide.enums.DayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleResponseDTO {

    private DayType day;
    private List<TimeSlotResponseDTO> timeSlots;

    public static ScheduleResponseDTO from(GuideSchedule guideSchedule) {
        return ScheduleResponseDTO.builder()
                .day(guideSchedule.getDay())
                .timeSlots(guideSchedule.getTimeSlots().stream()
                        .map(TimeSlotResponseDTO::from)
                        .toList())
                .build();
    }
}
