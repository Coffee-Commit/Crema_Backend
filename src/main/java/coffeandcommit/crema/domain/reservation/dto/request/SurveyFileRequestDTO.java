package coffeandcommit.crema.domain.reservation.dto.request;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SurveyFileRequestDTO {

    @Column(name = "file_upload_url", length = 2048)
    private String fileUploadUrl; // 파일 업로드 URL (S3 경로)
}
