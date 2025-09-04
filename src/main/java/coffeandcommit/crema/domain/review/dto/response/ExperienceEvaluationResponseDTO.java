package coffeandcommit.crema.domain.review.dto.response;

import coffeandcommit.crema.domain.review.entity.ReviewExperience;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExperienceEvaluationResponseDTO {

    private Long experienceGroupId;   // 경험 대주제 ID
    private String experienceTitle;   // 경험 대주제 제목
    private Boolean thumbsUp;         // 좋아요 여부

    public static ExperienceEvaluationResponseDTO from(ReviewExperience reviewExperience) {
        return ExperienceEvaluationResponseDTO.builder()
                .experienceGroupId(reviewExperience.getExperienceGroup().getId())
                .experienceTitle(reviewExperience.getExperienceGroup().getExperienceTitle())
                .thumbsUp(reviewExperience.isThumbsUp())
                .build();
    }
}
