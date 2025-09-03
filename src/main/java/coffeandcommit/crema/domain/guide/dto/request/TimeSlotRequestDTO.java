package coffeandcommit.crema.domain.guide.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TimeSlotRequestDTO {

    @NotBlank(message = "시작 시간은 필수 값입니다.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime startTime; // 시작 시간 (예: "09:00")

    @NotBlank(message = "종료 시간은 필수 값입니다.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime endTime;   // 종료 시간 (예: "10:00")
}
