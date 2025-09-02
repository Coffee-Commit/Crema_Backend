package coffeandcommit.crema.domain.guide.dto.request;

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
public class TimeSlotRequestDTO {

    @NotBlank(message = "시작 시간은 필수 값입니다.")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "시작 시간은 HH:mm 형식이어야 합니다.")
    private String startTime; // 시작 시간 (예: "09:00")

    @NotBlank(message = "종료 시간은 필수 값입니다.")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "종료 시간은 HH:mm 형식이어야 합니다.")
    private String endTime;   // 종료 시간 (예: "10:00")
}
