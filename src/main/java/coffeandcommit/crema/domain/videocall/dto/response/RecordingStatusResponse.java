package coffeandcommit.crema.domain.videocall.dto.response;

import io.openvidu.java.client.Recording;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingStatusResponse {
    
    private String sessionId;
    private boolean isRecording;
    private RecordingResponse currentRecording;
    private String message;
    
    public static RecordingStatusResponse from(String sessionId, boolean isRecording, Recording recording) {
        String message = isRecording ? "녹화가 진행 중입니다." : "현재 녹화 중이 아닙니다.";
        
        return RecordingStatusResponse.builder()
                .sessionId(sessionId)
                .isRecording(isRecording)
                .currentRecording(recording != null ? RecordingResponse.from(recording) : null)
                .message(message)
                .build();
    }
}