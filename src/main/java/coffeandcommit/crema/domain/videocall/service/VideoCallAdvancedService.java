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
            
            // 세션 설정 정보 포함하여 응답 (EC2 직접 연결)
            return QuickJoinResponse.builder()
                    .sessionId(session.getSessionId())
                    .sessionName(session.getSessionName())
                    .username(username)
                    .token(token)
                    .openviduServerUrl("https://crema.bitcointothemars.com/openvidu")
                    .apiBaseUrl("https://crema.bitcointothemars.com")
                    .webSocketUrl("wss://crema.bitcointothemars.com/openvidu")
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
                .openviduServerUrl("https://ec2-13-209-15-208.ap-northeast-2.compute.amazonaws.com:5443")
                .apiBaseUrl("https://crema.bitcointothemars.com")
                .webSocketUrl("wss://crema.bitcointothemars.com/openvidu")
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
                        .recordingEnabled(true) // 브라우저 기반 직접 녹화 활성화
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
                    .openviduServerUrl("https://crema.bitcointothemars.com/openvidu")
                    .apiBaseUrl("https://crema.bitcointothemars.com")
                    .webSocketUrl("wss://crema.bitcointothemars.com/openvidu")
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
                    .openviduServerUrl("https://crema.bitcointothemars.com/openvidu")
                    .apiBaseUrl("https://crema.bitcointothemars.com")
                    .webSocketUrl("wss://crema.bitcointothemars.com/openvidu")
                    .isReconnection(true)
                    .configInfo(buildConfigInfo())
                    .build();
            
        } catch (Exception e) {
            log.error("자동 재연결 실패: sessionId={}, username={}, error={}", 
                    sessionId, username, e.getMessage(), e);
            throw new RuntimeException("자동 재연결 중 오류가 발생했습니다. 네트워크 상태를 확인 후 다시 시도해주세요: " + e.getMessage());
        }
    }

    // 브라우저 기반 직접 녹화로 변경되어 서버 측 녹화 기능들은 간소화됨
    
    /**
     * [간소화됨] 브라우저 직접 녹화 사용 권장
     * 서버 측 녹화 대신 브라우저에서 직접 MediaStream 녹화를 사용하세요
     */
    @Deprecated
    public Recording startAudioRecording(String sessionId) {
        log.warn("서버 측 녹화는 비활성화됨. 브라우저 기반 직접 녹화를 사용하세요: sessionId={}", sessionId);
        throw new UnsupportedOperationException("서버 측 녹화는 브라우저 기반 직접 녹화로 대체되었습니다");
    }

    /**
     * [간소화됨] 브라우저 직접 녹화 사용 권장
     */
    @Deprecated
    public Recording stopAudioRecording(String sessionId) {
        log.warn("서버 측 녹화는 비활성화됨. 브라우저 기반 직접 녹화를 사용하세요: sessionId={}", sessionId);
        throw new UnsupportedOperationException("서버 측 녹화는 브라우저 기반 직접 녹화로 대체되었습니다");
    }

    /**
     * [간소화됨] 브라우저에서 직접 녹화한 파일들은 로컬에 저장됨
     */
    @Deprecated
    @Transactional(readOnly = true)
    public List<Recording> getSessionRecordings(String sessionId) {
        log.info("브라우저 기반 직접 녹화 사용 중 - 서버 측 녹화 목록 없음: sessionId={}", sessionId);
        return List.of(); // 빈 목록 반환
    }

    /**
     * [간소화됨] 브라우저 직접 녹화 파일 정보는 클라이언트에서 관리
     */
    @Deprecated  
    @Transactional(readOnly = true)
    public Recording getRecordingInfo(String recordingId) {
        log.warn("브라우저 기반 직접 녹화 사용 중 - 서버에서 녹화 정보 제공 불가: recordingId={}", recordingId);
        throw new UnsupportedOperationException("브라우저 기반 직접 녹화 파일은 클라이언트에서 관리됩니다");
    }

    /**
     * [간소화됨] 브라우저 기반 녹화 상태는 클라이언트에서 관리
     */
    @Deprecated
    @Transactional(readOnly = true)
    public boolean isSessionRecording(String sessionId) {
        log.info("브라우저 기반 직접 녹화 사용 중 - 녹화 상태 확인 불가: sessionId={}", sessionId);
        return false; // 서버에서는 녹화 상태를 알 수 없음
    }

    /**
     * [간소화됨] 브라우저 기반 활성 녹화 정보는 클라이언트에서 관리
     */
    @Deprecated
    @Transactional(readOnly = true)  
    public Recording getActiveRecording(String sessionId) {
        log.info("브라우저 기반 직접 녹화 사용 중 - 서버에서 활성 녹화 정보 제공 불가: sessionId={}", sessionId);
        return null; // 서버에서는 활성 녹화 정보 없음
    }

    /**
     * [간소화됨] 브라우저 기반 직접 녹화에서는 파일 다운로드가 브라우저에서 직접 처리됨
     */
    @Deprecated
    @Transactional(readOnly = true)
    public org.springframework.core.io.Resource getRecordingFile(String recordingId) {
        log.warn("브라우저 기반 직접 녹화에서는 파일이 로컬에 저장됩니다: recordingId={}", recordingId);
        throw new UnsupportedOperationException("브라우저 기반 직접 녹화 파일은 사용자 로컬에서 직접 관리됩니다");
    }

    /**
     * OpenVidu 서버 상태 확인
     */
    @Transactional(readOnly = true)
    public List<Session> getOpenViduStatus() {
        try {
            return videoCallService.getOpenViduActiveSessions();
        } catch (Exception e) {
            log.error("OpenVidu 상태 확인 실패: {}", e.getMessage());
            throw new RuntimeException("OpenVidu 서버에 연결할 수 없습니다: " + e.getMessage());
        }
    }

    /**
     * OpenVidu 시크릿 반환 (다운로드 인증용)
     */
    public String getOpenviduSecret() {
        return openviduSecret;
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