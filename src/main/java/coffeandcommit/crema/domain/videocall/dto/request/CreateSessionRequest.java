package coffeandcommit.crema.domain.videocall.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateSessionRequest {
    
    @NotBlank(message = "세션 이름은 필수입니다.")
    private String sessionName;
}