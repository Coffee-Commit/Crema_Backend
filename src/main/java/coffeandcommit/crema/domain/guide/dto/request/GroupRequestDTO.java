package coffeandcommit.crema.domain.guide.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupRequestDTO {

    @NotNull(message = "가이드 커피챗 주제 ID는 필수입니다.")
    private Long guideChatTopicId;

    @NotBlank(message = "경험 제목은 필수 입력 값입니다.")
    @Size(max = 40, message = "경험 제목은 최대 40자까지 가능합니다.")
    private String experienceTitle;

    @NotBlank(message = "경험 내용은 필수 입력 값입니다.")
    @Size(max = 200, message = "경험 내용은 최대 200자까지 가능합니다.")
    private String experienceContent;
}
