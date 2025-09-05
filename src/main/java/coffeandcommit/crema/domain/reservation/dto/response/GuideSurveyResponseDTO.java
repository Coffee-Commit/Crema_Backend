package coffeandcommit.crema.domain.reservation.dto.response;

import coffeandcommit.crema.domain.guide.dto.response.GuideChatTopicResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideJobFieldResponseDTO;
import coffeandcommit.crema.domain.guide.entity.Guide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GuideSurveyResponseDTO {

    private Long id;
    private String title;
    private String nickname;
    private String profileImageUrl;
    private String description;
    private GuideJobFieldResponseDTO jobField;
    private List<GuideChatTopicResponseDTO> chatTopics;

    public static GuideSurveyResponseDTO from(Guide guide) {
        return GuideSurveyResponseDTO.builder()
                .id(guide.getId())
                .title(guide.getTitle())
                .nickname(guide.getMember().getNickname())
                .profileImageUrl(guide.getMember().getProfileImageUrl())
                .description(guide.getMember().getDescription())
                .jobField(
                        guide.getGuideJobField() != null
                                ? GuideJobFieldResponseDTO.from(guide.getGuideJobField())
                                : null
                )
                .chatTopics(
                        guide.getGuideChatTopics().stream()
                                .map(GuideChatTopicResponseDTO::from)
                                .toList()
                )
                .build();
    }
}
