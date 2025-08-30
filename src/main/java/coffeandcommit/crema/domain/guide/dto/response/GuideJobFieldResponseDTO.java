package coffeandcommit.crema.domain.guide.dto.response;

import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.globalTag.enums.JobType;
import coffeandcommit.crema.domain.guide.entity.Guide;
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
    private JobType jobType;
    private JobNameType jobName;

    public static GuideJobFieldResponseDTO from(Guide guide, GuideJobField guideJobField) {
        return GuideJobFieldResponseDTO.builder()
                .guideId(guide.getId())
                .jobType(guideJobField.getJobField().getJobType())
                .jobName(guideJobField.getJobField().getJobName())
                .build();
    }

}
