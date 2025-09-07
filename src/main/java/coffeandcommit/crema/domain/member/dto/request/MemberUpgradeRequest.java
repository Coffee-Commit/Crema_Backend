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

    // PDF 업로드는 MultipartFile로 Controller에서 별도 처리
    // DTO에는 URL만 저장하는 필드가 필요한 경우를 위해 주석으로 남김
    // private String certificationPdfUrl;
}