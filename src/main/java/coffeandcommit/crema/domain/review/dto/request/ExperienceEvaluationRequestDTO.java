package coffeandcommit.crema.domain.review.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("thumbsUp")
    private Boolean isThumbsUp; // 좋아요 여부

}
