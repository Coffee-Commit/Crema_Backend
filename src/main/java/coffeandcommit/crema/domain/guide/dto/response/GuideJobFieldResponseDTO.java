package coffeandcommit.crema.domain.guide.dto.response;

import coffeandcommit.crema.domain.globalTag.dto.JobFieldDTO;
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
    private Long guideJobFieldId;
    private JobFieldDTO jobFieldDTO;

    public static GuideJobFieldResponseDTO from(GuideJobField guideJobField){
        return GuideJobFieldResponseDTO.builder()
                .guideId(guideJobField.getGuide().getId())
                .guideJobFieldId(guideJobField.getId())
                .jobFieldDTO(JobFieldDTO.from(guideJobField.getJobField()))
                .build();
    }

}
