package coffeandcommit.crema.domain.testvideocall.service;

import coffeandcommit.crema.domain.videocall.dto.response.QuickJoinResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionConfigResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionStatusResponse;
import coffeandcommit.crema.domain.videocall.entity.Participant;
import coffeandcommit.crema.domain.videocall.entity.VideoSession;
import coffeandcommit.crema.domain.videocall.exception.AutoReconnectFailedException;
import coffeandcommit.crema.domain.videocall.exception.OpenViduConnectionException;
import coffeandcommit.crema.domain.videocall.exception.SessionConnectFailed;
import coffeandcommit.crema.domain.videocall.exception.SessionNotFoundException;
import coffeandcommit.crema.domain.videocall.exception.TokenRefreshFailedException;
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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VideoCallTestService {

    @Value("${openvidu.domain}")
    private String openviduDomain;

    @Value("${openvidu.secret}")
    private String openviduSecret;

    private final BasicVideoCallTestService basicVideoCallTestService;
    
    private final VideoSessionRepository videoSessionRepository;

    public QuickJoinResponse quickJoin(String sessionName, String username) {
        long methodStartTime = System.currentTimeMillis();
        log.info("[QUICKJOIN-SERVICE] 테스트 quickJoin 서비스 시작 - sessionName: {}, username: {}", sessionName, username);
        
        try {
            VideoSession session;
            String fullSessionName = "test_" + sessionName;
            boolean isNewSession = false;
            
            log.info("[QUICKJOIN-SERVICE] 전체 세션 이름 생성: {} -> {}", sessionName, fullSessionName);
            
            try {
                // 기존 세션이 있는지 확인
                long sessionCheckStart = System.currentTimeMillis();
                log.info("[QUICKJOIN-SERVICE] 기존 세션 조회 시작 - fullSessionName: {}", fullSessionName);
                
                session = videoSessionRepository
                        .findBySessionNameAndIsActiveTrue(fullSessionName)
                        .orElseThrow(SessionNotFoundException::new);
                
                long sessionCheckTime = System.currentTimeMillis() - sessionCheckStart;
                log.info("[QUICKJOIN-SERVICE] 기존 세션 조회 성공 - sessionId: {}, 조회시간: {}ms", 
                         session.getSessionId(), sessionCheckTime);
                
            } catch (SessionNotFoundException e) {
                // 세션이 없으면 새로 생성
                long sessionCreateStart = System.currentTimeMillis();
                log.info("[QUICKJOIN-SERVICE] 기존 세션 없음, 새 세션 생성 시작 - fullSessionName: {}", fullSessionName);
                
                try {
                    session = basicVideoCallTestService.createVideoSession(fullSessionName);
                    isNewSession = true;
                    
                    long sessionCreateTime = System.currentTimeMillis() - sessionCreateStart;
                    log.info("[QUICKJOIN-SERVICE] 새 세션 생성 성공 - sessionId: {}, 생성시간: {}ms", 
                             session.getSessionId(), sessionCreateTime);
                } catch (Exception createError) {
                    long sessionCreateTime = System.currentTimeMillis() - sessionCreateStart;
                    log.error("[QUICKJOIN-SERVICE] 새 세션 생성 실패 - fullSessionName: {}, 시도시간: {}ms, error: {}", 
                              fullSessionName, sessionCreateTime, createError.getMessage(), createError);
                    throw createError;
                }
            }
            
            // 토큰 발급
            long tokenStart = System.currentTimeMillis();
            log.info("[QUICKJOIN-SERVICE] 토큰 발급 시작 - sessionId: {}, username: {}", session.getSessionId(), username);
            
            String token;
            try {
                // VideoSession 객체를 직접 전달하여 DB 재조회 방지
                token = basicVideoCallTestService.joinSession(session, username);
                long tokenTime = System.currentTimeMillis() - tokenStart;
                log.info("[QUICKJOIN-SERVICE] 토큰 발급 성공 - sessionId: {}, username: {}, 토큰길이: {}, 발급시간: {}ms", 
                         session.getSessionId(), username, token != null ? token.length() : 0, tokenTime);
            } catch (Exception tokenError) {
                long tokenTime = System.currentTimeMillis() - tokenStart;
                log.error("[QUICKJOIN-SERVICE] 토큰 발급 실패 - sessionId: {}, username: {}, 시도시간: {}ms, error: {}", 
                          session.getSessionId(), username, tokenTime, tokenError.getMessage(), tokenError);
                throw tokenError;
            }
            
            // 응답 구성
            long responseStart = System.currentTimeMillis();
            log.info("[QUICKJOIN-SERVICE] 응답 데이터 구성 시작");
            
            QuickJoinResponse response = QuickJoinResponse.builder()
                    .sessionId(session.getSessionId())
                    .sessionName(session.getSessionName())
                    .username(username)
                    .token(token)
                    .openviduServerUrl("https://" + openviduDomain)
                    .apiBaseUrl("https://" + openviduDomain)
                    .webSocketUrl("wss://" + openviduDomain)
                    .isNewSession(isNewSession || Duration.between(session.getCreatedAt(), LocalDateTime.now()).toMillis() < 5000)
                    .configInfo(buildConfigInfo())
                    .build();
            
            long responseTime = System.currentTimeMillis() - responseStart;
            long totalTime = System.currentTimeMillis() - methodStartTime;
            
            log.info("[QUICKJOIN-SERVICE] 테스트 quickJoin 서비스 완료 - sessionId: {}, username: {}, " +
                    "isNewSession: {}, 응답구성시간: {}ms, 전체처리시간: {}ms", 
                    response.getSessionId(), username, response.getIsNewSession(), responseTime, totalTime);
            
            return response;
            
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - methodStartTime;
            log.error("[QUICKJOIN-SERVICE] 테스트 세션 연결 실패 - sessionName: {}, username: {}, " +
                    "전체처리시간: {}ms, errorType: {}, error: {}", 
                    sessionName, username, totalTime, e.getClass().getSimpleName(), e.getMessage(), e);
            
            // 원본 예외가 이미 적절한 타입이면 그대로 던지고, 아니면 SessionConnectFailed로 래핑
            if (e instanceof SessionConnectFailed || e instanceof SessionNotFoundException) {
                throw e;
            }
            throw new SessionConnectFailed();
        }
    }

    public SessionConfigResponse getFrontendConfig() {
        return SessionConfigResponse.builder()
                .openviduServerUrl("https://" + openviduDomain)
                .apiBaseUrl("https://" + openviduDomain)
                .webSocketUrl("wss://" + openviduDomain)
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
                        .recordingEnabled(true)
                        .virtualBackgroundEnabled(false)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public SessionStatusResponse getSessionStatus(String sessionId) {
        VideoSession session = basicVideoCallTestService.getSession(sessionId);
        List<Participant> activeParticipants = basicVideoCallTestService.getActiveParticipants(sessionId);
        
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

    public QuickJoinResponse refreshToken(String sessionId, String username) {
        try {
            VideoSession session = basicVideoCallTestService.getSession(sessionId);
            
            String newToken = basicVideoCallTestService.joinSession(sessionId, username);
            
            return QuickJoinResponse.builder()
                    .sessionId(sessionId)
                    .sessionName(session.getSessionName())
                    .username(username)
                    .token(newToken)
                    .openviduServerUrl("https://" + openviduDomain)
                    .apiBaseUrl("https://" + openviduDomain)
                    .webSocketUrl("wss://" + openviduDomain)
                    .isTokenRefresh(true)
                    .configInfo(buildConfigInfo())
                    .build();
            
        } catch (Exception e) {
            log.error("테스트 토큰 갱신 실패: sessionId={}, username={}, error={}", 
                    sessionId, username, e.getMessage(), e);
            throw new TokenRefreshFailedException();
        }
    }

    public QuickJoinResponse autoReconnect(String sessionId, String username, String lastConnectionId) {
        try {
            if (lastConnectionId != null) {
                try {
                    basicVideoCallTestService.leaveSession(sessionId, lastConnectionId);
                    log.info("테스트 이전 연결 정리 완료: connectionId={}", lastConnectionId);
                } catch (Exception e) {
                    log.warn("테스트 이전 연결 정리 실패 (무시): connectionId={}, error={}", lastConnectionId, e.getMessage());
                }
            }
            
            VideoSession session = basicVideoCallTestService.getSession(sessionId);
            String newToken = basicVideoCallTestService.joinSession(sessionId, username);
            
            return QuickJoinResponse.builder()
                    .sessionId(sessionId)
                    .sessionName(session.getSessionName())
                    .username(username)
                    .token(newToken)
                    .openviduServerUrl("https://" + openviduDomain)
                    .apiBaseUrl("https://" + openviduDomain)
                    .webSocketUrl("wss://" + openviduDomain)
                    .isReconnection(true)
                    .configInfo(buildConfigInfo())
                    .build();
            
        } catch (Exception e) {
            log.error("테스트 자동 재연결 실패: sessionId={}, username={}, error={}", 
                    sessionId, username, e.getMessage(), e);
            throw new AutoReconnectFailedException();
        }
    }

    @Transactional(readOnly = true)
    public List<Session> getOpenViduStatus() {
        try {
            return basicVideoCallTestService.getOpenViduActiveSessions();
        } catch (Exception e) {
            log.error("테스트 OpenVidu 상태 확인 실패: {}", e.getMessage());
            throw new OpenViduConnectionException();
        }
    }

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