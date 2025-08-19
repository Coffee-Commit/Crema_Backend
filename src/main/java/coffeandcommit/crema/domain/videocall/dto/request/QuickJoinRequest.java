package coffeandcommit.crema.domain.videocall.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "원클릭 세션 참가 요청")
public class QuickJoinRequest {

    @Schema(description = "세션 이름 (기존 세션 참가 시) 또는 생성할 세션 이름", example = "개발팀 회의")
    private String sessionName;

    @NotBlank(message = "사용자명은 필수입니다")
    @Schema(description = "사용자명", example = "김개발", required = true)
    private String username;

    @Schema(description = "세션이 없을 때 자동 생성 여부", example = "true", defaultValue = "true")
    private Boolean autoCreateSession = true;

    @Schema(description = "세션 ID (기존 세션 직접 참가 시)", example = "session_1234567890")
    private String sessionId;
}