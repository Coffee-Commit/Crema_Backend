package coffeandcommit.crema.domain.reservation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SurveyFileRequestDTO {

    @NotBlank(message = "파일 업로드 URL은 필수입니다.")
    private String fileUploadUrl; // 파일 업로드 URL (S3 경로)
}
