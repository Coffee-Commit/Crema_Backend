package coffeandcommit.crema.domain.guide.dto.request;

import coffeandcommit.crema.domain.globalTag.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GuideJobFieldRequestDTO {

    private JobType jobType;
}
