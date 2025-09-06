package coffeandcommit.crema.domain.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class MemberUpgradeResponse {

    // 가이드 관련 정보만
    private Long guideId;
    private String companyName;
    private Boolean isCompanyNamePublic;
    private String jobPosition;
    private Boolean isCurrent;
    private LocalDate workingStart;
    private LocalDate workingEnd;
    private int workingPeriodYears;
    private boolean isApproved; // 승인 상태
    private boolean isOpened; // 프로필 공개 여부
    private String title; // 가이드 제목
    private String chatDescription; // 가이드 설명
}