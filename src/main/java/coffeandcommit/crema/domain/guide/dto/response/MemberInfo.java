package coffeandcommit.crema.domain.guide.dto.response;

import coffeandcommit.crema.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberInfo {

    private String nickname;          // 멘티 닉네임
    private String profileImageUrl;   // 멘티 프로필 이미지 URL

    public static MemberInfo from(Member member) {
        return MemberInfo.builder()
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .build();
    }
}
