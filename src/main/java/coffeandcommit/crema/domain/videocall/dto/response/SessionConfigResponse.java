package coffeandcommit.crema.domain.videocall.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "프론트엔드 설정 정보 응답")
public class SessionConfigResponse {

    @Schema(description = "OpenVidu 서버 URL", example = "https://crema.bitcointothemars.com")
    private String openviduServerUrl;

    @Schema(description = "백엔드 API 기본 URL", example = "https://crema.bitcointothemars.com")
    private String apiBaseUrl;

    @Schema(description = "WebSocket URL", example = "wss://crema.bitcointothemars.com/openvidu")
    private String webSocketUrl;

    @Schema(description = "기본 비디오 설정")
    private VideoConfig defaultVideoConfig;

    @Schema(description = "지원되는 브라우저 목록")
    private List<String> supportedBrowsers;

    @Schema(description = "활성화된 기능들")
    private Features features;

    @Getter
    @Builder
    @Schema(description = "비디오 설정")
    public static class VideoConfig {
        
        @Schema(description = "해상도", example = "640x480")
        private String resolution;

        @Schema(description = "프레임 레이트", example = "30")
        private Integer frameRate;

        @Schema(description = "오디오 발행 여부", example = "true")
        private Boolean publishAudio;

        @Schema(description = "비디오 발행 여부", example = "true")
        private Boolean publishVideo;
    }

    @Getter
    @Builder
    @Schema(description = "기능 활성화 상태")
    public static class Features {
        
        @Schema(description = "채팅 기능", example = "true")
        private Boolean chatEnabled;

        @Schema(description = "화면 공유 기능", example = "true")
        private Boolean screenShareEnabled;

        @Schema(description = "녹화 기능", example = "false")
        private Boolean recordingEnabled;

        @Schema(description = "가상 배경 기능", example = "false")
        private Boolean virtualBackgroundEnabled;
    }
}