package coffeandcommit.crema.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberUpgradeRequest {

    @NotBlank(message = "회사명은 필수 입력 값입니다.")
    @Size(max = 16, message = "회사명은 최대 16자까지 가능합니다.")
    private String companyName;

    @NotNull(message = "회사명 공개 여부는 필수입니다.")
    private Boolean isCompanyNamePublic;

    @NotBlank(message = "직무명은 필수 입력 값입니다.")
    @Size(max = 16, message = "직무명은 최대 16자까지 가능합니다.")
    private String jobPosition;

    @NotNull(message = "재직중 여부는 필수입니다.")
    private Boolean isCurrent;

    @NotNull(message = "근무 시작일은 필수입니다.")
    private LocalDate workingStart;

    private LocalDate workingEnd; // 재직중이면 null

    // TODO: PDF 업로드 필드 추가 예정
    // private String certificationPdfUrl;
}