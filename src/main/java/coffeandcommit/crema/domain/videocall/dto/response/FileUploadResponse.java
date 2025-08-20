package coffeandcommit.crema.domain.videocall.dto.response;

import coffeandcommit.crema.domain.videocall.entity.UploadedFile;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FileUploadResponse {
    private Long id;
    private String sessionId;
    private String fileName;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private String uploader;
    private LocalDateTime uploadedAt;
    private String downloadUrl;
    
    public static FileUploadResponse from(UploadedFile uploadedFile) {
        return FileUploadResponse.builder()
                .id(uploadedFile.getId())
                .sessionId(uploadedFile.getSessionId())
                .fileName(uploadedFile.getFileName())
                .originalFileName(uploadedFile.getOriginalFileName())
                .fileType(uploadedFile.getFileType())
                .fileSize(uploadedFile.getFileSize())
                .uploader(uploadedFile.getUploader())
                .uploadedAt(uploadedFile.getUploadedAt())
                .downloadUrl("/api/files/" + uploadedFile.getId())
                .build();
    }
}