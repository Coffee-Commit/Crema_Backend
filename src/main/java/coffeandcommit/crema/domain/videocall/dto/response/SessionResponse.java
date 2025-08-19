package coffeandcommit.crema.domain.videocall.dto.response;

import coffeandcommit.crema.domain.videocall.entity.VideoSession;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SessionResponse {
    private Long id;
    private String sessionId;
    private String sessionName;
    private LocalDateTime createdAt;
    private Boolean isActive;
    
    public static SessionResponse from(VideoSession videoSession) {
        return SessionResponse.builder()
                .id(videoSession.getId())
                .sessionId(videoSession.getSessionId())
                .sessionName(videoSession.getSessionName())
                .createdAt(videoSession.getCreatedAt())
                .isActive(videoSession.getIsActive())
                .build();
    }
}