package coffeandcommit.crema.domain.videocall.dto.response;

import io.openvidu.java.client.Recording;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingResponse {
    
    private String recordingId;
    private String sessionId;
    private String name;
    private Recording.Status status;
    private double duration; // 초 단위
    private long size; // 바이트 단위
    private String url;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
    private boolean hasAudio;
    private boolean hasVideo;
    private String outputMode;
    
    public static RecordingResponse from(Recording recording) {
        return RecordingResponse.builder()
                .recordingId(recording.getId())
                .sessionId(recording.getSessionId())
                .name(recording.getName())
                .status(recording.getStatus())
                .duration(recording.getDuration())
                .size(recording.getSize())
                .url(recording.getUrl())
                .createdAt(recording.getCreatedAt() > 0 ? 
                    LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(recording.getCreatedAt()), 
                        ZoneId.systemDefault()) : null)
                .hasAudio(recording.hasAudio())
                .hasVideo(recording.hasVideo())
                .outputMode(recording.getOutputMode() != null ? recording.getOutputMode().name() : "COMPOSED")
                .build();
    }
}