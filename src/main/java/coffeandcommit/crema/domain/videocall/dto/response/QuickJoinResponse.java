package coffeandcommit.crema.domain.videocall.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuickJoinResponse {
    private String sessionId;
    private String sessionName;
    private String username;
    private String token;
    private String openviduServerUrl;
    private String apiBaseUrl;
    private String webSocketUrl;
    private Boolean isNewSession;
    private Boolean isTokenRefresh;
    private Boolean isReconnection;
    private ConfigInfo configInfo;
    
    @Data
    @Builder
    public static class ConfigInfo {
        private String defaultResolution;
        private Integer defaultFrameRate;
        private Boolean autoEnableAudio;
        private Boolean autoEnableVideo;
        private Boolean chatEnabled;
    }
}
