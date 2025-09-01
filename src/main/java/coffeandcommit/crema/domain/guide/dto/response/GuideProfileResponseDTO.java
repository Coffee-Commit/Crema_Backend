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
    private Long guideId;              // Guide PK (Long)
    private String memberId;           // Member PK (UUID String, CHAR(8))
    private String nickname;           // member.nickname
    private String profileImageUrl;    // member.profile_image_url
    private String companyName;        // guide.company_name
    private LocalDate workingStart;  // 근무 시작일
    private LocalDate workingEnd;    // 근무 종료일 (null이면 현재 재직 중)
    private int workingPeriodYears;      // 연차
    private boolean isOpened;              // 가이드 프로필 노출 여부
    private GuideJobFieldResponseDTO guideJobField;

    public static GuideProfileResponseDTO from(Guide guide, int workingPeriodYears, GuideJobFieldResponseDTO guideJobFieldResponseDTO) {
        return GuideProfileResponseDTO.builder()
                .guideId(guide.getId())
                .memberId(guide.getMember().getId())
                .nickname(guide.getMember().getNickname())
                .profileImageUrl(guide.getMember().getProfileImageUrl())
                .companyName(guide.getCompanyName())
                .workingStart(guide.getWorkingStart())
                .workingEnd(guide.getWorkingEnd())
                .workingPeriodYears(workingPeriodYears)
                .isOpened(guide.isOpened())
                .guideJobField(guideJobFieldResponseDTO)
                .build();
    }

}
