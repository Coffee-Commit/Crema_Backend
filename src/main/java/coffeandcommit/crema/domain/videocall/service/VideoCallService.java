package coffeandcommit.crema.domain.videocall.service;

import coffeandcommit.crema.domain.videocall.dto.response.QuickJoinResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionJoinResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionStatusResponse;
import coffeandcommit.crema.domain.videocall.entity.VideoSession;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VideoCallService {
    public QuickJoinResponse quickJoin() {
        return null;
    }

    public SessionJoinResponse getFrontendConfig() {
        return null;
    }

    public SessionStatusResponse getSessionStatus() {
        return null;
    }

    public QuickJoinResponse refreshToken(String sessionId, String username) {
        return null;
    }

    public QuickJoinResponse autoReconnect(String sessionId, String username, String lastConnectionId) {
        return null;
    }

    public List<VideoSession> getOpenViduStatus() {
        return null;
    }
}
