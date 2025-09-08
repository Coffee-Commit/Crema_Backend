package coffeandcommit.crema.domain.reservation.dto.response;

import coffeandcommit.crema.domain.guide.entity.Guide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GuideDTO {

    private Long id;
    private String title;
    private String nickname;
    private String profileImageUrl;

    public static GuideDTO from(Guide guide) {
        return GuideDTO.builder()
                .id(guide.getId())
                .title(guide.getTitle())
                .nickname(guide.getMember().getNickname())
                .profileImageUrl(guide.getMember().getProfileImageUrl())
                .build();
    }
}
