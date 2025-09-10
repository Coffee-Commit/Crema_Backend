package coffeandcommit.crema.domain.videocall.dto.response;

import coffeandcommit.crema.domain.videocall.dto.ChatMessageDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatHistoryResponse {

    private String sessionId;
    private List<ChatMessageDto> messages;
    private Integer totalMessages;
    private LocalDateTime sessionStartTime;
    private LocalDateTime sessionEndTime;
    private LocalDateTime createdAt;
}