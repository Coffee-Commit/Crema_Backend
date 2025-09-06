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

    private String companyName;
    private Boolean isCompanyNamePublic;
    private String jobPosition;
    private Boolean isCurrent;
    private LocalDate workingStart;
    private LocalDate workingEnd;

    private String workingPeriod; // "2년 3개월" 또는 "2021.07 ~ 재직중"
    private int workingPeriodYears;
    private int workingPeriodMonths;

    private boolean isApproved;
    private boolean isOpened;
    private String title;
    private String chatDescription;
}