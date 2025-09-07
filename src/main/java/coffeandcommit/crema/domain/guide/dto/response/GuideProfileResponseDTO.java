package coffeandcommit.crema.domain.guide.dto.response;

import coffeandcommit.crema.domain.guide.entity.Guide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideProfileResponseDTO {
    private Long guideId;              // 가이드 ID
    private String nickname;           // 닉네임 (member.nickname)
    private String profileImageUrl;    // 프로필 이미지 URL
    private String companyName;        // 회사명
    private String workingPeriod;      // "n년 n개월" 형태의 근무기간
    private GuideJobFieldResponseDTO guideJobField; // 직무분야 정보

    public static GuideProfileResponseDTO from(Guide guide, String workingPeriod, GuideJobFieldResponseDTO guideJobField) {
        return GuideProfileResponseDTO.builder()
                .guideId(guide.getId())
                .nickname(guide.getMember().getNickname())
                .profileImageUrl(guide.getMember().getProfileImageUrl())
                .companyName(guide.getCompanyName())
                .workingPeriod(workingPeriod)
                .guideJobField(guideJobField)
                .build();
    }

}
