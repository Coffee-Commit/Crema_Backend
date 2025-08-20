package coffeandcommit.crema.domain.videocall.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatMessageRequest {
    
    @NotBlank(message = "세션 ID는 필수입니다.")
    private String sessionId;
    
    @NotBlank(message = "사용자 이름은 필수입니다.")
    private String username;
    
    @NotBlank(message = "메시지는 필수입니다.")
    @Size(max = 1000, message = "메시지는 1000자를 초과할 수 없습니다.")
    private String message;
}