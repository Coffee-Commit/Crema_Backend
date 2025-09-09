package coffeandcommit.crema.domain.videocall.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {

    private String timestamp;
    private String participantId;
    private String participantName;
    private String message;
    private String messageType;
}