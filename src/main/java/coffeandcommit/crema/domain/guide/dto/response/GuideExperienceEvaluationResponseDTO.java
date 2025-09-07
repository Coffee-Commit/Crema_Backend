package coffeandcommit.crema.domain.guide.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideExperienceEvaluationResponseDTO {

    private Long experienceId;      // 경험 대주제 ID
    private String experienceTitle; // 경험 제목
    private String thumbsUpRate;    // 따봉 비율 (예: "80%")

    public static GuideExperienceEvaluationResponseDTO of(Long experienceId, String experienceTitle, double rate) {
        // 소수점 버리고 정수 + % 붙여서 반환
        String formattedRate = String.format("%.0f%%", rate * 100);
        return GuideExperienceEvaluationResponseDTO.builder()
                .experienceId(experienceId)
                .experienceTitle(experienceTitle)
                .thumbsUpRate(formattedRate)
                .build();
    }
}
