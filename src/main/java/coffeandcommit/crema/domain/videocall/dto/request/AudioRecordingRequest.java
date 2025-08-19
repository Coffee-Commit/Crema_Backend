package coffeandcommit.crema.domain.videocall.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioRecordingRequest {
    
    @NotBlank(message = "세션 ID는 필수입니다.")
    private String sessionId;
    
    private String recordingName; // 선택적 녹화 파일명
    
    private String description; // 녹화 설명
}