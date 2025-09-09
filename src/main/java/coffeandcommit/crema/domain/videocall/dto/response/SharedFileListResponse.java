package coffeandcommit.crema.domain.videocall.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "공유 파일 목록 응답 DTO")
public class SharedFileListResponse {
    
    @Schema(description = "공유 파일 목록")
    private List<SharedFileResponse> materials;
    
    @Schema(description = "총 파일 개수", example = "5")
    private long totalCount;
    
    @Schema(description = "세션 ID", example = "session123")
    private String sessionId;
    
    /**
     * 공유 파일 목록과 총 개수로 응답 생성
     */
    public static SharedFileListResponse of(List<SharedFileResponse> materials, long totalCount, String sessionId) {
        return SharedFileListResponse.builder()
                .materials(materials)
                .totalCount(totalCount)
                .sessionId(sessionId)
                .build();
    }
}