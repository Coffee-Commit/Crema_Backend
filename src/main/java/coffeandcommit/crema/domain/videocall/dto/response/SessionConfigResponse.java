package coffeandcommit.crema.domain.videocall.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SessionConfigResponse {
    private String openviduServerUrl;
    private String apiBaseUrl;
    private String webSocketUrl;
    private VideoConfig defaultVideoConfig;
    private List<String> supportedBrowsers;
    private Features features;
    
    @Data
    @Builder
    public static class VideoConfig {
        private String resolution;
        private Integer frameRate;
        private Boolean publishAudio;
        private Boolean publishVideo;
    }
    
    @Data
    @Builder
    public static class Features {
        private Boolean chatEnabled;
        private Boolean screenShareEnabled;
        private Boolean recordingEnabled;
        private Boolean virtualBackgroundEnabled;
    }
}