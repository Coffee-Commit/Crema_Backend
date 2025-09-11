package coffeandcommit.crema.domain.guide.dto.request;

import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
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
public class GroupRequestDTO {

    // enum 주제명으로 식별 (예: INTERVIEW 또는 "면접")
    // enum 주제명 입력값 (없거나 파싱 실패 시 서비스 레이어에서 INVALID_TOPIC 반환)
    private TopicNameType topicName;

    @NotBlank(message = "경험 제목은 필수 입력 값입니다.")
    @Size(max = 40, message = "경험 제목은 최대 40자까지 가능합니다.")
    private String experienceTitle;

    @NotBlank(message = "경험 내용은 필수 입력 값입니다.")
    @Size(max = 200, message = "경험 내용은 최대 200자까지 가능합니다.")
    private String experienceContent;
}
