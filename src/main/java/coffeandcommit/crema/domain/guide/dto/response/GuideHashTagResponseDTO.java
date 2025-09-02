package coffeandcommit.crema.domain.guide.dto.response;

import coffeandcommit.crema.domain.guide.entity.HashTag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideHashTagResponseDTO {

    private Long id;
    private Long guideId;
    private String hashTagName;

    public static GuideHashTagResponseDTO from(HashTag hashTag, Long guideId) {
        return GuideHashTagResponseDTO.builder()
                .id(hashTag.getId())
                .guideId(guideId)
                .hashTagName(hashTag.getHashTagName())
                .build();
    }
}
