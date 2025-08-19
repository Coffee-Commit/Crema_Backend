package coffeandcommit.crema.domain.videocall.service;

import coffeandcommit.crema.domain.videocall.entity.Participant;
import coffeandcommit.crema.domain.videocall.entity.VideoSession;
import coffeandcommit.crema.domain.videocall.exception.ParticipantNotFoundException;
import coffeandcommit.crema.domain.videocall.exception.SessionCreationException;
import coffeandcommit.crema.domain.videocall.exception.SessionNotFoundException;
import coffeandcommit.crema.domain.videocall.repository.ParticipantRepository;
import coffeandcommit.crema.domain.videocall.repository.VideoSessionRepository;
import io.openvidu.java.client.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VideoCallService {

    @Value("${openvidu.url}")
    private String openviduUrl;

    @Value("${openvidu.secret}")
    private String openviduSecret;

    private OpenVidu openVidu;

    private final VideoSessionRepository videoSessionRepository;
    private final ParticipantRepository participantRepository;

    @PostConstruct
    private void init() {
        this.openVidu = new OpenVidu(openviduUrl, openviduSecret);
    }

    public VideoSession createSession(String sessionName) {
        try {
            String sessionId = "session_" + System.currentTimeMillis();
            
            SessionProperties sessionProperties = new SessionProperties.Builder()
                    .customSessionId(sessionId)
                    .mediaMode(MediaMode.ROUTED)
                    .recordingMode(RecordingMode.MANUAL)
                    .build();

            Session openviduSession = openVidu.createSession(sessionProperties);
            
            VideoSession videoSession = VideoSession.builder()
                    .sessionId(sessionId)
                    .sessionName(sessionName)
                    .build();

            return videoSessionRepository.save(videoSession);
            
        } catch (Exception e) {
            log.error("세션 생성 실패: {}", e.getMessage());
            throw new SessionCreationException("세션 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public String joinSession(String sessionId, String username) {
        try {
            VideoSession videoSession = videoSessionRepository
                    .findBySessionIdAndIsActiveTrue(sessionId)
                    .orElseThrow(() -> new SessionNotFoundException("활성화된 세션을 찾을 수 없습니다."));

            Session openviduSession = openVidu.getActiveSession(sessionId);
            if (openviduSession == null) {
                openviduSession = openVidu.createSession(
                    new SessionProperties.Builder()
                            .customSessionId(sessionId)
                            .build()
                );
            }

            ConnectionProperties connectionProperties = new ConnectionProperties.Builder()
                    .type(ConnectionType.WEBRTC)
                    .data("{\"username\":\"" + username + "\"}")
                    .role(OpenViduRole.PUBLISHER)
                    .build();

            Connection connection = openviduSession.createConnection(connectionProperties);
            String token = connection.getToken();
            
            log.info("Generated token: {}", token);
            log.info("Connection ID: {}", connection.getConnectionId());

            Participant participant = Participant.builder()
                    .connectionId(connection.getConnectionId())
                    .token(token)
                    .username(username)
                    .videoSession(videoSession)
                    .build();

            participantRepository.save(participant);

            return token;

        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            log.error("세션 참가 실패: {}", e.getMessage());
            throw new SessionCreationException("세션 참가 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public void leaveSession(String sessionId, String connectionId) {
        try {
            Participant participant = participantRepository
                    .findByConnectionId(connectionId)
                    .orElseThrow(() -> new ParticipantNotFoundException());

            participant.leaveSession();
            participantRepository.save(participant);

            Session openviduSession = openVidu.getActiveSession(sessionId);
            if (openviduSession != null) {
                openviduSession.forceDisconnect(connectionId);
            }

        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            log.error("세션 나가기 실패: {}", e.getMessage());
        }
    }

    public void endSession(String sessionId) {
        try {
            VideoSession videoSession = videoSessionRepository
                    .findBySessionId(sessionId)
                    .orElseThrow(() -> new SessionNotFoundException());

            videoSession.endSession();
            videoSessionRepository.save(videoSession);

            List<Participant> participants = participantRepository
                    .findByVideoSessionAndIsConnectedTrue(videoSession);
            
            participants.forEach(Participant::leaveSession);
            participantRepository.saveAll(participants);

            Session openviduSession = openVidu.getActiveSession(sessionId);
            if (openviduSession != null) {
                openviduSession.close();
            }

        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            log.error("세션 종료 실패: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public VideoSession getSession(String sessionId) {
        return videoSessionRepository
                .findBySessionId(sessionId)
                .orElseThrow(() -> new SessionNotFoundException());
    }

    @Transactional(readOnly = true)
    public List<Participant> getActiveParticipants(String sessionId) {
        VideoSession videoSession = getSession(sessionId);
        return participantRepository.findByVideoSessionAndIsConnectedTrue(videoSession);
    }
}