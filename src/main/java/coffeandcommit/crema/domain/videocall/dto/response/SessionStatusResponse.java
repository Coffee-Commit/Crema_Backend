package coffeandcommit.crema.domain.videocall.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SessionStatusResponse {
    private String sessionId;
    private String sessionName;
    private Boolean isActive;
    private Integer participantCount;
    private List<ParticipantInfo> participants;
    private LocalDateTime createdAt;
    
    @Data
    @Builder
    public static class ParticipantInfo {
        private String username;
        private String connectionId;
        private LocalDateTime joinedAt;
        private Boolean isConnected;
    }
}
