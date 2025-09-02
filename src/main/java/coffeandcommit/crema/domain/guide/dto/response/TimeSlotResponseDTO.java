package coffeandcommit.crema.domain.guide.dto.response;

import coffeandcommit.crema.domain.guide.entity.TimeSlot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TimeSlotResponseDTO {

    private Long id;
    private String startTime; // 시작 시간 (예: "09:00")
    private String endTime;   // 종료 시간 (예: "10:00")

    public static TimeSlotResponseDTO from(TimeSlot timeSlot) {
        return TimeSlotResponseDTO.builder()
                .id(timeSlot.getId())
                .startTime(timeSlot.getStartTimeOption().toString())
                .endTime(timeSlot.getEndTimeOption().toString())
                .build();
    }
}
