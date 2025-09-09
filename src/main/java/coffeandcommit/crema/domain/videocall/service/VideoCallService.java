package coffeandcommit.crema.domain.videocall.service;

import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.repository.ReservationRepository;
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
import org.springframework.security.core.userdetails.UserDetails;
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
public class VideoCallService {

    @Value("${openvidu.domain}")
    private String openviduDomain;

    @Value("${openvidu.secret}")
    private String openviduSecret;

    private final BasicVideoCallService basicVideoCallService;
    
    private final VideoSessionRepository videoSessionRepository;

    private final ReservationRepository reservationRepository;

    public QuickJoinResponse quickJoin(Long reservationId, UserDetails userDetails) {
        try {
            Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(SessionNotFoundException::new);

            VideoSession session;
            try {   //세션이 없으면 새로 만듦
                if(reservation.getVideoSession() == null)
                    throw new SessionNotFoundException();

                session = videoSessionRepository
                        .findBySessionNameAndIsActiveTrue(reservation.getVideoSession().getSessionName())
                        .orElseThrow(SessionNotFoundException::new);
            }catch (SessionNotFoundException e) {
                String sessionName = "reservation_" + reservation.getId() + "_" + reservation.getReservedAt();
                session = basicVideoCallService.createVideoSession(sessionName);
            }
            
            String token = basicVideoCallService.joinSession(session.getSessionId(), userDetails.getUsername());
            
            return QuickJoinResponse.builder()
                    .sessionId(session.getSessionId())
                    .sessionName(session.getSessionName())
                    .username(userDetails.getUsername())
                    .token(token)
                    .openviduServerUrl("https://" + openviduDomain)
                    .apiBaseUrl("https://" + openviduDomain)
                    .webSocketUrl("wss://" + openviduDomain)
                    .isNewSession(Duration.between(session.getCreatedAt(), LocalDateTime.now()).toMillis() < 5000)
                    .configInfo(buildConfigInfo())
                    .build();
            
        } catch (Exception e) {
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
        VideoSession session = basicVideoCallService.getSession(sessionId);
        List<Participant> activeParticipants = basicVideoCallService.getActiveParticipants(sessionId);
        
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
            VideoSession session = basicVideoCallService.getSession(sessionId);
            
            String newToken = basicVideoCallService.joinSession(sessionId, username);
            
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
            log.error("토큰 갱신 실패: sessionId={}, username={}, error={}", 
                    sessionId, username, e.getMessage(), e);
            throw new TokenRefreshFailedException();
        }
    }

    public QuickJoinResponse autoReconnect(String sessionId, String username, String lastConnectionId) {
        try {
            if (lastConnectionId != null) {
                try {
                    basicVideoCallService.leaveSession(sessionId, lastConnectionId);
                    log.info("이전 연결 정리 완료: connectionId={}", lastConnectionId);
                } catch (Exception e) {
                    log.warn("이전 연결 정리 실패 (무시): connectionId={}, error={}", lastConnectionId, e.getMessage());
                }
            }
            
            VideoSession session = basicVideoCallService.getSession(sessionId);
            String newToken = basicVideoCallService.joinSession(sessionId, username);
            
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
            log.error("자동 재연결 실패: sessionId={}, username={}, error={}", 
                    sessionId, username, e.getMessage(), e);
            throw new AutoReconnectFailedException();
        }
    }

    @Transactional(readOnly = true)
    public List<Session> getOpenViduStatus() {
        try {
            return basicVideoCallService.getOpenViduActiveSessions();
        } catch (Exception e) {
            log.error("OpenVidu 상태 확인 실패: {}", e.getMessage());
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
