package coffeandcommit.crema.domain.review.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExperienceEvaluationRequestDTO {

    @NotNull
    private Long experienceGroupId;

    @NotNull
    private Boolean thumbsUp; // 좋아요 여부

}
