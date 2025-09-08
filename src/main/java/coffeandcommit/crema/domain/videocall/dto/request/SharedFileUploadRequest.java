package coffeandcommit.crema.domain.videocall.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "공유 파일 등록 요청 DTO")
public class SharedFileUploadRequest {
    
    @Schema(description = "S3에 업로드된 파일의 imageKey", example = "shared-materials/user123_20241208_1733654321_document.pdf")
    @NotBlank(message = "이미지 키는 필수입니다")
    @Size(max = 500, message = "이미지 키는 500자를 초과할 수 없습니다")
    private String imageKey;
    
    @Schema(description = "파일 이름", example = "중요문서.pdf")
    @NotBlank(message = "파일 이름은 필수입니다")
    @Size(max = 255, message = "파일 이름은 255자를 초과할 수 없습니다")
    private String fileName;
    
    @Schema(description = "파일 크기 (바이트)", example = "1024000")
    @NotNull(message = "파일 크기는 필수입니다")
    @Positive(message = "파일 크기는 양수여야 합니다")
    private Long fileSize;
    
    @Schema(description = "파일 MIME 타입", example = "application/pdf")
    @Size(max = 100, message = "컨텐츠 타입은 100자를 초과할 수 없습니다")
    private String contentType;
}