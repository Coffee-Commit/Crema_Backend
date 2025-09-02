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
    private JobNameType jobName;

    public static GuideJobFieldResponseDTO from(GuideJobField guideJobField, Long guideId) {
        return GuideJobFieldResponseDTO.builder()
                .guideId(guideId)
                .jobName(guideJobField.getJobName())
                .build();
    }

}
