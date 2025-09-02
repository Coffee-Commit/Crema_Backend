package coffeandcommit.crema.domain.videocall.dto.request;

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
public class ChatHistorySaveRequest {

    private List<ChatMessageDto> messages;
    private LocalDateTime sessionStartTime;
    private LocalDateTime sessionEndTime;
}