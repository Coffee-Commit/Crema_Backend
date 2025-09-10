package coffeandcommit.crema.domain.guide.dto.response;

import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.guide.entity.GuideJobField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideJobFieldResponseDTO {

    private Long guideId;
    private JobNameType jobName; // 기존 호환 유지 (enum)
    private String jobNameDescription; // 프론트 표시용 설명 문자열

    public static GuideJobFieldResponseDTO from(GuideJobField guideJobField) {
        JobNameType type = guideJobField.getJobName() != null
                ? guideJobField.getJobName()
                : JobNameType.UNDEFINED;

        return GuideJobFieldResponseDTO.builder()
                .guideId(guideJobField.getGuide().getId())
                .jobName(type)
                .jobNameDescription(type.getDescription())
                .build();
    }

}
