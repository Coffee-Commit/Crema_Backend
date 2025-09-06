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

    private Long guideId;
    private String companyName;
    private Boolean isCompanyNamePublic;
    private String jobPosition;
    private Boolean isCurrent;

    private String workingPeriod; // "2년 3개월" 또는 "2021.07 ~ 재직중"
    private int workingPeriodYears; // 숫자 연차
    private int workingPeriodMonths; // 개월 수 추가
    private LocalDate workingStart;
    private LocalDate workingEnd;

    private boolean isApproved; // 승인 상태
    private boolean isOpened; // 프로필 공개 여부
    private String title; // 가이드 제목
    private String chatDescription; // 가이드 설명
}