package coffeandcommit.crema.domain.videocall.dto.request;

import coffeandcommit.crema.domain.videocall.dto.ChatMessageDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotEmpty(message = "채팅 메시지는 비어있을 수 없습니다.")
    @Size(max = 1000, message = "채팅 메시지는 최대 1000개까지 저장 가능합니다.")
    @Valid
    private List<ChatMessageDto> messages;
    
    @NotNull(message = "세션 시작 시간은 필수입니다.")
    private LocalDateTime sessionStartTime;
    
    private LocalDateTime sessionEndTime;
}