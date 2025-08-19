package coffeandcommit.crema.domain.videocall.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "실시간 세션 상태 응답")
public class SessionStatusResponse {

    @Schema(description = "세션 ID", example = "session_1234567890")
    private String sessionId;

    @Schema(description = "세션 이름", example = "개발팀 회의")
    private String sessionName;

    @Schema(description = "세션 활성 상태", example = "true")
    private Boolean isActive;

    @Schema(description = "현재 참가자 수", example = "3")
    private Integer participantCount;

    @Schema(description = "참가자 목록")
    private List<ParticipantInfo> participants;

    @Schema(description = "세션 생성 시간")
    private LocalDateTime createdAt;

    @Getter
    @Builder
    @Schema(description = "참가자 정보")
    public static class ParticipantInfo {
        
        @Schema(description = "사용자명", example = "김개발")
        private String username;

        @Schema(description = "연결 ID")
        private String connectionId;

        @Schema(description = "참가 시간")
        private LocalDateTime joinedAt;

        @Schema(description = "연결 상태", example = "true")
        private Boolean isConnected;
    }
}