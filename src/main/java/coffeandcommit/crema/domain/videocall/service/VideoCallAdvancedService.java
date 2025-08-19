package coffeandcommit.crema.domain.videocall.service;

import coffeandcommit.crema.domain.videocall.dto.response.QuickJoinResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionConfigResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionStatusResponse;
import coffeandcommit.crema.domain.videocall.entity.Participant;
import coffeandcommit.crema.domain.videocall.entity.VideoSession;
import coffeandcommit.crema.domain.videocall.repository.ParticipantRepository;
import coffeandcommit.crema.domain.videocall.repository.VideoSessionRepository;
import io.openvidu.java.client.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 프론트엔드 개발자의 작업을 간소화하기 위한 고급 화상통화 서비스
 * 복잡한 OpenVidu 로직을 백엔드에서 처리하여 프론트엔드는 간단한 API 호출만으로 화상통화 구현 가능
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VideoCallAdvancedService {

    @Value("${openvidu.url}")
    private String openviduUrl;

    @Value("${openvidu.secret}")
    private String openviduSecret;

    @Value("${server.port}")
    private String serverPort;

    private final VideoCallService videoCallService;
    private final VideoSessionRepository videoSessionRepository;
    private final ParticipantRepository participantRepository;

    /**
     * 원클릭 세션 참가 - 프론트엔드에서 한 번의 API 호출로 모든 세션 설정 완료
     */
    public QuickJoinResponse quickJoin(String sessionName, String username, Boolean autoCreateSession) {
        try {
            VideoSession session = null;
            
            // 세션 이름으로 기존 세션 찾기
            if (sessionName != null) {
                session = videoSessionRepository
                        .findBySessionNameAndIsActiveTrue(sessionName)
                        .orElse(null);
            }
            
            // 세션이 없고 자동 생성이 활성화된 경우 새 세션 생성
            if (session == null && Boolean.TRUE.equals(autoCreateSession)) {
                session = videoCallService.createSession(sessionName != null ? sessionName : "Auto-Session-" + System.currentTimeMillis());
                log.info("새 세션 자동 생성: {}", session.getSessionId());
            } else if (session == null) {
                throw new RuntimeException("세션을 찾을 수 없습니다: " + sessionName);
            }
            
            // 세션 참가 및 토큰 발급
            String token = videoCallService.joinSession(session.getSessionId(), username);
            
            // 세션 설정 정보 포함하여 응답
            return QuickJoinResponse.builder()
                    .sessionId(session.getSessionId())
                    .sessionName(session.getSessionName())
                    .username(username)
                    .token(token)
                    .openviduServerUrl(openviduUrl)
                    .apiBaseUrl("http://localhost:" + serverPort)
                    .webSocketUrl(openviduUrl.replace("http://", "ws://"))
                    .isNewSession(java.time.Duration.between(session.getCreatedAt(), java.time.LocalDateTime.now()).toMillis() < 5000) // 5초 내 생성된 세션
                    .configInfo(buildConfigInfo())
                    .build();
            
        } catch (Exception e) {
            log.error("원클릭 참가 실패: sessionName={}, username={}, error={}", 
                    sessionName, username, e.getMessage(), e);
            
            if (e.getMessage().contains("세션을 찾을 수 없습니다")) {
                throw new RuntimeException("지정된 세션이 존재하지 않습니다. 세션 이름을 확인해주세요.");
            } else if (e.getMessage().contains("Connection")) {
                throw new RuntimeException("OpenVidu 서버에 연결할 수 없습니다. 서버 상태를 확인해주세요.");
            } else {
                throw new RuntimeException("화상통화 참가 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
    }

    /**
     * 프론트엔드에 필요한 모든 설정 정보 제공
     */
    public SessionConfigResponse getFrontendConfig() {
        return SessionConfigResponse.builder()
                .openviduServerUrl(openviduUrl)
                .apiBaseUrl("http://localhost:" + serverPort)
                .webSocketUrl(openviduUrl.replace("http://", "ws://"))
                .defaultVideoConfig(SessionConfigResponse.VideoConfig.builder()
                        .resolution("640x480")
                        .frameRate(30)
                        .publishAudio(true)
                        .publishVideo(true)
                        .build())
                .supportedBrowsers(List.of("Chrome", "Firefox", "Safari", "Edge"))
                .features(SessionConfigResponse.Features.builder()
                        .chatEnabled(true)
                        .screenShareEnabled(true)
                        .recordingEnabled(false)
                        .virtualBackgroundEnabled(false)
                        .build())
                .build();
    }

    /**
     * 실시간 세션 상태 조회 - 프론트엔드에서 세션 모니터링 간편화
     */
    @Transactional(readOnly = true)
    public SessionStatusResponse getSessionStatus(String sessionId) {
        VideoSession session = videoCallService.getSession(sessionId);
        List<Participant> activeParticipants = videoCallService.getActiveParticipants(sessionId);
        
        return SessionStatusResponse.builder()
                .sessionId(sessionId)
                .sessionName(session.getSessionName())
                .isActive(session.getIsActive())
                .participantCount(activeParticipants.size())
                .participants(activeParticipants.stream()
                        .map(participant -> SessionStatusResponse.ParticipantInfo.builder()
                                .username(participant.getUsername())
                                .connectionId(participant.getConnectionId())
                                .joinedAt(participant.getJoinedAt())
                                .isConnected(participant.getIsConnected())
                                .build())
                        .collect(Collectors.toList()))
                .createdAt(session.getCreatedAt())
                .build();
    }

    /**
     * 토큰 자동 갱신 - 프론트엔드에서 토큰 관리 부담 제거
     */
    public QuickJoinResponse refreshToken(String sessionId, String username) {
        try {
            // 기존 참가자 정보 조회
            VideoSession session = videoCallService.getSession(sessionId);
            
            // 새 토큰 발급 (기존 연결은 유지)
            String newToken = videoCallService.joinSession(sessionId, username);
            
            return QuickJoinResponse.builder()
                    .sessionId(sessionId)
                    .sessionName(session.getSessionName())
                    .username(username)
                    .token(newToken)
                    .openviduServerUrl(openviduUrl)
                    .apiBaseUrl("http://localhost:" + serverPort)
                    .webSocketUrl(openviduUrl.replace("http://", "ws://"))
                    .isTokenRefresh(true)
                    .configInfo(buildConfigInfo())
                    .build();
            
        } catch (Exception e) {
            log.error("토큰 갱신 실패: sessionId={}, username={}, error={}", 
                    sessionId, username, e.getMessage(), e);
            throw new RuntimeException("토큰 갱신 중 오류가 발생했습니다. 다시 시도해주세요: " + e.getMessage());
        }
    }

    /**
     * 자동 재연결 처리 - 네트워크 오류 시 프론트엔드의 복잡한 재연결 로직 대신 간단한 API 호출
     */
    public QuickJoinResponse autoReconnect(String sessionId, String username, String lastConnectionId) {
        try {
            // 이전 연결 정리
            if (lastConnectionId != null) {
                try {
                    videoCallService.leaveSession(sessionId, lastConnectionId);
                    log.info("이전 연결 정리 완료: connectionId={}", lastConnectionId);
                } catch (Exception e) {
                    log.warn("이전 연결 정리 실패 (무시): connectionId={}, error={}", lastConnectionId, e.getMessage());
                }
            }
            
            // 새로운 연결 생성
            VideoSession session = videoCallService.getSession(sessionId);
            String newToken = videoCallService.joinSession(sessionId, username);
            
            return QuickJoinResponse.builder()
                    .sessionId(sessionId)
                    .sessionName(session.getSessionName())
                    .username(username)
                    .token(newToken)
                    .openviduServerUrl(openviduUrl)
                    .apiBaseUrl("http://localhost:" + serverPort)
                    .webSocketUrl(openviduUrl.replace("http://", "ws://"))
                    .isReconnection(true)
                    .configInfo(buildConfigInfo())
                    .build();
            
        } catch (Exception e) {
            log.error("자동 재연결 실패: sessionId={}, username={}, error={}", 
                    sessionId, username, e.getMessage(), e);
            throw new RuntimeException("자동 재연결 중 오류가 발생했습니다. 네트워크 상태를 확인 후 다시 시도해주세요: " + e.getMessage());
        }
    }

    /**
     * 프론트엔드를 위한 설정 정보 빌드
     */
    private QuickJoinResponse.ConfigInfo buildConfigInfo() {
        return QuickJoinResponse.ConfigInfo.builder()
                .defaultResolution("640x480")
                .defaultFrameRate(30)
                .autoEnableAudio(true)
                .autoEnableVideo(true)
                .chatEnabled(true)
                .build();
    }
}