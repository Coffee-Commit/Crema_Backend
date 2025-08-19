package coffeandcommit.crema.domain.videocall.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "원클릭 세션 참가 응답 - 프론트엔드에서 즉시 OpenVidu 연결 가능한 모든 정보 포함")
public class QuickJoinResponse {

    @Schema(description = "세션 ID", example = "session_1234567890")
    private String sessionId;

    @Schema(description = "세션 이름", example = "개발팀 회의")
    private String sessionName;

    @Schema(description = "사용자명", example = "김개발")
    private String username;

    @Schema(description = "OpenVidu 연결 토큰")
    private String token;

    @Schema(description = "OpenVidu 서버 URL", example = "http://localhost:25565")
    private String openviduServerUrl;

    @Schema(description = "백엔드 API 기본 URL", example = "http://localhost:8081")
    private String apiBaseUrl;

    @Schema(description = "WebSocket URL", example = "ws://localhost:25565")
    private String webSocketUrl;

    @Schema(description = "새로 생성된 세션 여부", example = "true")
    private Boolean isNewSession;

    @Schema(description = "토큰 갱신 요청 여부", example = "false")
    private Boolean isTokenRefresh;

    @Schema(description = "재연결 요청 여부", example = "false")
    private Boolean isReconnection;

    @Schema(description = "추가 설정 정보")
    private ConfigInfo configInfo;

    @Getter
    @Builder
    @Schema(description = "프론트엔드 설정 정보")
    public static class ConfigInfo {
        
        @Schema(description = "비디오 해상도", example = "640x480")
        private String defaultResolution;

        @Schema(description = "프레임 레이트", example = "30")
        private Integer defaultFrameRate;

        @Schema(description = "자동 오디오 활성화", example = "true")
        private Boolean autoEnableAudio;

        @Schema(description = "자동 비디오 활성화", example = "true")
        private Boolean autoEnableVideo;

        @Schema(description = "채팅 기능 활성화", example = "true")
        private Boolean chatEnabled;
    }
}