package coffeandcommit.crema.domain.guide.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GuideExperienceDetailRequestDTO {

    @NotBlank(message = "누구에게 필드는 필수 입력 값입니다.")
    @Size(max = 40, message = "누구에게는 최대 40자까지 가능합니다.")
    private String who;

    @NotBlank(message = "어떤 문제 해결에 필드는 필수 입력 값입니다.")
    @Size(max = 40, message = "문제 해결은 최대 40자까지 가능합니다.")
    private String solution;

    @NotBlank(message = "어떻게 필드는 필수 입력 값입니다.")
    @Size(max = 40, message = "방법은 최대 40자까지 가능합니다.")
    private String how;
}
