package coffeandcommit.crema.domain.guide.dto.response;

import coffeandcommit.crema.domain.guide.entity.ExperienceDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideExperienceDetailResponseDTO {

    private Long id;
    private Long guideId;
    private String who;
    private String solution;
    private String how;

    public static GuideExperienceDetailResponseDTO from(ExperienceDetail experienceDetail) {
        return GuideExperienceDetailResponseDTO.builder()
                .id(experienceDetail.getId())
                .guideId(experienceDetail.getGuide().getId())
                .who(experienceDetail.getWho())
                .solution(experienceDetail.getSolution())
                .how(experienceDetail.getHow())
                .build();
    }
}
