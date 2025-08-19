package coffeandcommit.crema.domain.videocall.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JoinSessionRequest {
    
    @NotBlank(message = "사용자명은 필수입니다.")
    private String username;
}