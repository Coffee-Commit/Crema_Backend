package coffeandcommit.crema.domain.guide.dto.response;

import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.reservation.dto.response.GuideDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideCoffeeChatResponseDTO {

    private GuideDTO guide; // 가이드 요약 정보

    @JsonProperty("isOpened")
    private boolean isOpened;

    private String title;
    private String chatDescription;

    private List<GuideHashTagResponseDTO> tags; // 해시태그 목록

    private Double reviewScore;
    private Long reviewCount;

    private GuideExperienceResponseDTO experiences; // 경험 그룹 (groups 배열 포함)

    private GuideExperienceDetailResponseDTO experienceDetail; // 경험 상세 단건

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GuideCoffeeChatResponseDTO from(
            Guide guide,
            List<GuideHashTagResponseDTO> tags,
            Double reviewScore,
            Long reviewCount,
            GuideExperienceResponseDTO experiences,
            GuideExperienceDetailResponseDTO experienceDetail,
            boolean isOpened
    ) {
        return GuideCoffeeChatResponseDTO.builder()
                .guide(GuideDTO.from(guide))
                .title(guide.getTitle())
                .chatDescription(guide.getChatDescription())
                .tags(tags)
                .reviewScore(reviewScore)
                .reviewCount(reviewCount)
                .experiences(experiences)
                .experienceDetail(experienceDetail)
                .isOpened(isOpened)
                .createdAt(guide.getCreatedAt())
                .updatedAt(guide.getModifiedAt())
                .build();
    }
}
