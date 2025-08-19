package coffeandcommit.crema.domain.videocall.dto.response;

import io.openvidu.java.client.Recording;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingListResponse {
    
    private String sessionId;
    private int totalCount;
    private List<RecordingResponse> recordings;
    private boolean hasActiveRecording;
    private RecordingResponse activeRecording;
    
    public static RecordingListResponse from(String sessionId, List<Recording> recordings, Recording activeRecording) {
        return RecordingListResponse.builder()
                .sessionId(sessionId)
                .totalCount(recordings.size())
                .recordings(recordings.stream()
                        .map(RecordingResponse::from)
                        .collect(Collectors.toList()))
                .hasActiveRecording(activeRecording != null)
                .activeRecording(activeRecording != null ? RecordingResponse.from(activeRecording) : null)
                .build();
    }
}