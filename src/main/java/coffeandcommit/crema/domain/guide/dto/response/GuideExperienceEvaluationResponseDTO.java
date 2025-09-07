package coffeandcommit.crema.domain.guide.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
        double safe = Double.isFinite(rate) ? rate : 0.0;
        double clamped = Math.max(0.0, Math.min(1.0, safe));

        // BigDecimal 기반 내림 처리
        int percent = BigDecimal.valueOf(clamped)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.FLOOR)
                .intValue();

        // 퍼센트 문자열 생성
        String formattedRate = percent + "%";
        return GuideExperienceEvaluationResponseDTO.builder()
                .experienceGroupId(experienceGroupId)
                .experienceTitle(experienceTitle)
                .thumbsUpRate(formattedRate)
                .build();
    }
}
