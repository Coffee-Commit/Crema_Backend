package coffeandcommit.crema.domain.videocall.dto.request;

import lombok.Data;

@Data
public class FileUploadRequest {
    private String sessionId;
    private String username;
    private String fileName;
    private String fileType;
    private Long fileSize;
}