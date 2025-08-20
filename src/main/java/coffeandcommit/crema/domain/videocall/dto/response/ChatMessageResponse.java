package coffeandcommit.crema.domain.videocall.dto.response;

import coffeandcommit.crema.domain.videocall.entity.ChatMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {
    
    private Long id;
    private String sessionId;
    private String username;
    private String message;
    private LocalDateTime createdAt;
    
    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return ChatMessageResponse.builder()
                .id(chatMessage.getId())
                .sessionId(chatMessage.getSessionId())
                .username(chatMessage.getUsername())
                .message(chatMessage.getMessage())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }
}