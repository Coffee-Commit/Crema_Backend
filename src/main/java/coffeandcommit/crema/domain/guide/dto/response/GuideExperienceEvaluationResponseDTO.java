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

    private Long experienceGroupId;      // 경험 대주제 ID
    private String experienceTitle; // 경험 제목
    private String thumbsUpRate;    // 따봉 비율 (예: "80%")

    public static GuideExperienceEvaluationResponseDTO of(Long experienceGroupId, String experienceTitle, double rate) {
        // [0,1] 범위로 클램핑
        double clamped = Math.max(0.0, Math.min(1.0, rate));

        // 내림 처리 후 정수 변환
        int percent = (int) Math.floor(clamped * 100.0);

        // 퍼센트 문자열 생성
        String formattedRate = percent + "%";
        return GuideExperienceEvaluationResponseDTO.builder()
                .experienceGroupId(experienceGroupId)
                .experienceTitle(experienceTitle)
                .thumbsUpRate(formattedRate)
                .build();
    }
}
