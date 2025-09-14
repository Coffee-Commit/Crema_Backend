package coffeandcommit.crema.domain.videocall.service;

import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.repository.ReservationRepository;
import coffeandcommit.crema.domain.videocall.dto.response.ParticipantInfoResponse;
import coffeandcommit.crema.domain.videocall.dto.response.QuickJoinResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionConfigResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionStatusResponse;
import coffeandcommit.crema.domain.videocall.entity.Participant;
import coffeandcommit.crema.domain.videocall.entity.VideoSession;
import coffeandcommit.crema.domain.videocall.exception.*;
import coffeandcommit.crema.domain.videocall.repository.VideoSessionRepository;
import coffeandcommit.crema.domain.reservation.enums.Status;
import coffeandcommit.crema.domain.videocall.dto.request.ChatHistorySaveRequest;
import coffeandcommit.crema.domain.videocall.repository.ParticipantRepository;

import io.openvidu.java.client.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static coffeandcommit.crema.domain.member.enums.MemberRole.ROOKIE;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VideoCallService {

    private final MemberRepository memberRepository;
    @Value("${openvidu.domain}")
    private String openviduDomain;

    @Value("${openvidu.secret}")
    private String openviduSecret;

    private final BasicVideoCallService basicVideoCallService;
    
    private final VideoSessionRepository videoSessionRepository;

    private final ReservationRepository reservationRepository;
    
    private final ChatService chatService;
    
    private final ParticipantRepository participantRepository;

    public QuickJoinResponse quickJoin(Long reservationId, UserDetails userDetails) {
        try {
            Reservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new SessionNotFoundException("예약 ID: " + reservationId + "를 찾을 수 없습니다"));

            VideoSession session;
            try {   //세션이 없으면 새로 만듦
                if(reservation.getVideoSession() == null)
                    throw new SessionNotFoundException("예약 ID " + reservation.getId() + "에 연결된 VideoSession이 없습니다");

                session = videoSessionRepository
                        .findBySessionName(reservation.getVideoSession().getSessionName())
                        .orElseThrow(() -> new SessionNotFoundException("세션 이름: " + reservation.getVideoSession().getSessionName() + "를 찾을 수 없습니다"));
            }catch (SessionNotFoundException e) {
                log.info("[SESSION-QUICKJOIN] =======Exception======= \n{}", e.toString());
                String sessionName = "reservation_" + reservation.getId();
                session = basicVideoCallService.createVideoSession(sessionName);
            }
            
            String token = basicVideoCallService.joinSession(session.getSessionId(), userDetails.getUsername());
            Member member = memberRepository.findByIdAndIsDeletedFalse(userDetails.getUsername()).orElseThrow(ParticipantNotFound::new);

            return QuickJoinResponse.builder()
                    .sessionId(session.getSessionId())
                    .sessionName(session.getSessionName())
                    .username(member.getNickname())
                    .token(token)
                    .openviduServerUrl("https://" + openviduDomain)
                    .apiBaseUrl("https://" + openviduDomain)
                    .webSocketUrl("wss://" + openviduDomain)
                    .isNewSession(Duration.between(session.getCreatedAt(), LocalDateTime.now()).toMillis() < 5000)
                    .configInfo(buildConfigInfo())
                    .build();
            
        } catch (Exception e) {
            throw e;
        }
    }

    public QuickJoinResponse testQuickJoinAuth(String inputSessionName, UserDetails userDetails) {
        try {

            VideoSession session;
            try {   //세션이 없으면 새로 만듦
                session = videoSessionRepository
                        .findBySessionName(inputSessionName)
                        .orElseThrow(() -> new SessionNotFoundException("세션 이름: " + inputSessionName + "를 찾을 수 없습니다"));
            }catch (SessionNotFoundException e) {
                String sessionName = inputSessionName;
                session = basicVideoCallService.createVideoSession(sessionName);
            }

            String token = basicVideoCallService.joinSession(session.getSessionId(), userDetails.getUsername());
            Member member = memberRepository.findByIdAndIsDeletedFalse(userDetails.getUsername()).orElseThrow(ParticipantNotFound::new);


            return QuickJoinResponse.builder()
                    .sessionId(session.getSessionId())
                    .sessionName(session.getSessionName())
                    .username(member.getNickname())
                    .token(token)
                    .openviduServerUrl("https://" + openviduDomain)
                    .apiBaseUrl("https://" + openviduDomain)
                    .webSocketUrl("wss://" + openviduDomain)
                    .isNewSession(Duration.between(session.getCreatedAt(), LocalDateTime.now()).toMillis() < 5000)
                    .configInfo(buildConfigInfo())
                    .build();

        } catch (Exception e) {
            throw e;
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
            throw new TokenRefreshFailedException("세션 ID: " + sessionId + ", 사용자: " + username + " - " + e.getMessage());
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
            throw new AutoReconnectFailedException("세션 ID: " + sessionId + ", 사용자: " + username + ", 이전 연결: " + lastConnectionId + " - " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<Session> getOpenViduStatus() {
        try {
            return basicVideoCallService.getOpenViduActiveSessions();
        } catch (Exception e) {
            log.error("OpenVidu 상태 확인 실패: {}", e.getMessage());
            throw new OpenViduConnectionException("OpenVidu 서버 상태 확인 실패: " + e.getMessage());
        }
    }

    public ParticipantInfoResponse getParticipantInfo(String sessionId, String username) {

        Member member = memberRepository.findByIdAndIsDeletedFalse(username)
                .orElseThrow(ParticipantNotFound::new);
        VideoSession videoSession = videoSessionRepository.findBySessionId(sessionId)
                .orElseThrow(SessionNotFoundException::new);
        Reservation reservation = videoSession.getReservation();

        String oppponent = null;

        Guide guide = reservation.getGuide();
        Member mentee = reservation.getMember();

        if(member.getRole() == ROOKIE){
            oppponent = guide.getMember().getNickname();
        }else{
            oppponent = mentee.getNickname();
        }

        return ParticipantInfoResponse.builder()
                .participantName(oppponent)
                .videoChatField(guide.getGuideJobField().getJobName().toString())
                .videoChatTopic(guide.getGuideChatTopics().toString())
                .build();
    }

    @Transactional
    public void endSession(String sessionId, ChatHistorySaveRequest chatHistory, String username) {
        try {
            // 1. Member 조회 및 역할 확인
            Member member = memberRepository.findByNicknameAndIsDeletedFalse(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
            
            log.info("세션 종료 요청: sessionId={}, username={}, role={}", sessionId, username, member.getRole());
            
            // 2. ROOKIE인 경우 - 본인만 세션에서 나가기
            if (member.getRole() == ROOKIE) {
                // Participant 정보 조회
                Participant participant = participantRepository
                        .findByVideoSession_SessionIdAndUsername(sessionId, username)
                        .orElseThrow(() -> new RuntimeException("참가자 정보를 찾을 수 없습니다: sessionId=" + sessionId + ", username=" + username));
                
                // OpenVidu 세션에서 나가기만 함
                try {
                    basicVideoCallService.leaveSession(sessionId, participant.getConnectionId());
                    log.info("ROOKIE가 세션에서 나감: sessionId={}, username={}, connectionId={}", 
                            sessionId, username, participant.getConnectionId());
                } catch (Exception e) {
                    log.error("ROOKIE 세션 나가기 실패: sessionId={}, username={}, error={}", 
                            sessionId, username, e.getMessage());
                    throw new RuntimeException("세션 나가기 실패: " + e.getMessage(), e);
                }
                return; // 채팅 저장이나 예약 완료 없이 종료
            }
            
            // 3. GUIDE인 경우 - 전체 세션 종료 (기존 로직)
            log.info("GUIDE가 전체 세션 종료 시작: sessionId={}, username={}", sessionId, username);
            
            // VideoSession 조회
            VideoSession videoSession = videoSessionRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> new SessionNotFoundException("세션 ID: " + sessionId + "를 찾을 수 없습니다"));
            
            // OpenVidu 세션 종료
            try {
                basicVideoCallService.endSession(sessionId);
                log.info("GUIDE가 OpenVidu 세션 종료: sessionId={}", sessionId);
            } catch (Exception e) {
                log.error("OpenVidu 세션 종료 실패 (계속 진행): sessionId={}, error={}", sessionId, e.getMessage());
            }
            
            // 세션 종료 시간 기록
            videoSession.endSession();
            log.info("세션 종료 시간 기록: sessionId={}, endedAt={}", sessionId, videoSession.getEndedAt());
            
            // 채팅 기록 저장
            chatService.saveChatHistory(sessionId, chatHistory, username);
            log.info("채팅 기록 저장 완료: sessionId={}, username={}", sessionId, username);
            try {
                // 예약 상태 COMPLETED로 변경
                if (videoSession.getReservation() != null) {
                    Reservation reservation = videoSession.getReservation();
                    if (reservation.getStatus() != Status.COMPLETED) {
                        reservation.completeReservation();
                        log.info("예약 상태를 COMPLETED로 변경: reservationId={}", reservation.getId());
                    } else {
                        log.debug("예약이 이미 완료 상태입니다: reservationId={}", reservation.getId());
                    }
                } else {
                    log.warn("세션에 연결된 예약이 없습니다: sessionId={}", sessionId);
                }
            }catch (Exception e) {
                throw e;
            }
            
            // DB 저장
            videoSessionRepository.save(videoSession);
            log.info("GUIDE가 전체 세션 종료 완료: sessionId={}, username={}", sessionId, username);
            
        } catch (SessionNotFoundException e) {
            log.error("세션을 찾을 수 없음: sessionId={}, username={}", sessionId, username, e);
            throw e;
        } catch (SecurityException e) {
            log.error("세션 종료 권한 없음: sessionId={}, username={}", sessionId, username, e);
            throw e;
        } catch (Exception e) {
            log.error("세션 종료 처리 실패: sessionId={}, username={}", sessionId, username, e);
            throw new RuntimeException("세션 종료 처리 중 오류 - 세션 ID: " + sessionId + ", 사용자: " + username + ", 원인: " + e.getMessage(), e);
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
