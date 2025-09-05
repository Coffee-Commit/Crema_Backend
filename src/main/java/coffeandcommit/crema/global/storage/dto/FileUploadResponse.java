package coffeandcommit.crema.global.storage.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileUploadResponse {
    private String fileKey;
    private String fileUrl;
}