package coffeandcommit.crema.domain.review.dto.response;

import coffeandcommit.crema.domain.guide.entity.Guide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideInfo {

    private String nickname;         // 가이드 닉네임
    private String profileImageUrl;

    public static GuideInfo from(Guide guide) {
        return GuideInfo.builder()
                .nickname(guide.getMember().getNickname())
                .profileImageUrl(guide.getMember().getProfileImageUrl())
                .build();
    }
}
