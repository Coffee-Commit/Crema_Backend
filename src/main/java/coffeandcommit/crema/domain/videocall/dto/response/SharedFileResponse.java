package coffeandcommit.crema.domain.videocall.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "공유 파일 정보 응답 DTO")
public class SharedFileResponse {
    
    @Schema(description = "파일 ID", example = "1")
    private Long id;
    
    @Schema(description = "S3 이미지 키", example = "shared-materials/user123_20241208_1733654321_document.pdf")
    private String imageKey;
    
    @Schema(description = "파일 이름", example = "중요문서.pdf")
    private String fileName;
    
    @Schema(description = "파일 크기 (바이트)", example = "1024000")
    private Long fileSize;
    
    @Schema(description = "파일 MIME 타입", example = "application/pdf")
    private String contentType;
    
    @Schema(description = "업로드한 사용자 ID", example = "user123")
    private String uploadedByUserId;
    
    @Schema(description = "업로드한 사용자 이름", example = "김구직")
    private String uploadedByName;
    
    @Schema(description = "업로드 시간", example = "2024-12-08T15:35:00")
    private LocalDateTime uploadedAt;
    
    /**
     * VideoCallSharedFile 엔티티로부터 DTO 생성
     */
    public static SharedFileResponse from(coffeandcommit.crema.domain.videocall.entity.VideoCallSharedFile entity) {
        return SharedFileResponse.builder()
                .id(entity.getId())
                .imageKey(entity.getImageKey())
                .fileName(entity.getFileName())
                .fileSize(entity.getFileSize())
                .contentType(entity.getContentType())
                .uploadedByUserId(entity.getUploadedByUserId())
                .uploadedByName(entity.getUploadedByName())
                .uploadedAt(entity.getUploadedAt())
                .build();
    }
}