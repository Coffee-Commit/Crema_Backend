package coffeandcommit.crema.domain.videocall.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JoinSessionResponse {
    private String token;
    private String sessionId;
    private String username;
}