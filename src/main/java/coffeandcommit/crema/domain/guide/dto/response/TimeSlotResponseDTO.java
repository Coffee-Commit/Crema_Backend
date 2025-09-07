package coffeandcommit.crema.domain.guide.dto.response;

import coffeandcommit.crema.domain.guide.entity.TimeSlot;
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
    private String preferredTimeRange;

    public static TimeSlotResponseDTO from(TimeSlot timeSlot) {
        return TimeSlotResponseDTO.builder()
                .id(timeSlot.getId())
                .preferredTimeRange(
                        timeSlot.getStartTimeOption().toString() + " ~ " + timeSlot.getEndTimeOption().toString()
                )
                .build();
    }
}
