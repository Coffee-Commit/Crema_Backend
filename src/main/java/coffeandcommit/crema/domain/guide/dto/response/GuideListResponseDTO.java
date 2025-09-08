package coffeandcommit.crema.domain.guide.dto.response;

import coffeandcommit.crema.domain.guide.entity.Guide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideListResponseDTO {

    private Long guideId;
    private String nickname;
    private String profileImageUrl;
    private String title;
    private String workingPeriod; // "n년 n개월"

    private GuideJobFieldResponseDTO jobField;
    private List<GuideHashTagResponseDTO> hashTags;

    private CoffeeChatStatsResponseDTO stats;

    public static GuideListResponseDTO from(
            Guide guide,
            String workingPeriod,
            GuideJobFieldResponseDTO jobField,
            List<GuideHashTagResponseDTO> hashTags,
            CoffeeChatStatsResponseDTO stats
    ) {
        return GuideListResponseDTO.builder()
                .guideId(guide.getId())
                .nickname(guide.getMember().getNickname())
                .profileImageUrl(guide.getMember().getProfileImageUrl())
                .title(guide.getTitle())
                .workingPeriod(workingPeriod)
                .jobField(jobField)
                .hashTags(hashTags)
                .stats(stats)
                .build();
    }
}
