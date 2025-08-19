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
import java.util.concurrent.ConcurrentHashMap;

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
    
    // 녹화 세션 관리를 위한 맵
    private final Map<String, Recording> activeRecordings = new ConcurrentHashMap<>();

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

    public Recording startAudioRecording(String sessionId) {
        try {
            VideoSession videoSession = videoSessionRepository
                    .findBySessionId(sessionId)
                    .orElseThrow(() -> new SessionNotFoundException("세션을 찾을 수 없습니다."));

            // 이미 녹화 중인지 확인
            if (activeRecordings.containsKey(sessionId)) {
                throw new IllegalStateException("이미 녹화가 진행 중입니다.");
            }

            // OpenVidu에서 모든 활성 세션 확인 (디버깅용)
            log.info("모든 활성 세션 조회 시작");
            try {
                var allSessions = openVidu.getActiveSessions();
                log.info("전체 활성 세션 수: {}", allSessions.size());
                allSessions.forEach(session -> {
                    log.info("활성 세션 - ID: {}, 연결 수: {}", 
                            session.getSessionId(), 
                            session.getActiveConnections().size());
                });
            } catch (Exception e) {
                log.error("활성 세션 목록 조회 실패: {}", e.getMessage());
            }

            // OpenVidu 세션 상태 확인
            Session openviduSession = null;
            try {
                openviduSession = openVidu.getActiveSession(sessionId);
                log.info("getActiveSession 결과: {}", openviduSession != null ? "세션 존재" : "세션 없음");
            } catch (Exception e) {
                log.error("getActiveSession 호출 중 오류: {}", e.getMessage());
            }

            if (openviduSession == null) {
                log.warn("OpenVidu 세션을 찾을 수 없음: sessionId={}", sessionId);
                
                // 세션이 없는 경우 바로 녹화 시도 (OpenVidu가 자동으로 처리할 수 있음)
                log.info("세션이 없지만 녹화 시도해보기: sessionId={}", sessionId);
                
                // 음성만 녹화하도록 설정 (세션 체크 없이)
                RecordingProperties recordingProperties = new RecordingProperties.Builder()
                        .name("audio_recording_" + sessionId + "_" + System.currentTimeMillis())
                        .outputMode(Recording.OutputMode.COMPOSED)
                        .hasAudio(true)
                        .hasVideo(false)
                        .build();

                try {
                    Recording recording = openVidu.startRecording(sessionId, recordingProperties);
                    activeRecordings.put(sessionId, recording);
                    log.info("세션 체크 없이 녹화 시작 성공: sessionId={}, recordingId={}", sessionId, recording.getId());
                    return recording;
                } catch (Exception recordingException) {
                    log.error("세션 체크 없는 녹화 시도도 실패: {}", recordingException.getMessage());
                    throw new IllegalStateException("OpenVidu 세션이 활성화되어 있지 않습니다. 참가자가 세션에 연결되어 있는지 확인하세요.");
                }
            }

            // 세션에 연결된 참가자가 있는지 확인
            int connectionCount = 0;
            try {
                connectionCount = openviduSession.getActiveConnections().size();
                log.info("세션 연결 수 확인: sessionId={}, connectionCount={}", sessionId, connectionCount);
            } catch (Exception e) {
                log.error("연결 수 확인 중 오류: {}", e.getMessage());
            }

            // 연결 수가 0이어도 녹화 시도 (OpenVidu가 처리)
            log.info("녹화 시작 준비: sessionId={}, 연결된 참가자 수={}", sessionId, connectionCount);
            
            if (connectionCount == 0) {
                log.warn("참가자가 연결되어 있지 않지만 녹화 시도: sessionId={}", sessionId);
            }

            // 음성만 녹화하도록 설정
            RecordingProperties recordingProperties = new RecordingProperties.Builder()
                    .name("audio_recording_" + sessionId + "_" + System.currentTimeMillis())
                    .outputMode(Recording.OutputMode.COMPOSED)
                    .hasAudio(true)
                    .hasVideo(false)  // 비디오는 녹화하지 않음
                    .build();

            Recording recording = openVidu.startRecording(sessionId, recordingProperties);
            
            // 활성 녹화 목록에 추가
            activeRecordings.put(sessionId, recording);
            
            log.info("음성 녹화 시작 성공: sessionId={}, recordingId={}, 참가자 수={}", 
                    sessionId, recording.getId(), connectionCount);
            
            return recording;
            
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            log.error("OpenVidu 녹화 시작 실패: sessionId={}, error={}, message={}", 
                    sessionId, e.getClass().getSimpleName(), e.getMessage());
            
            String errorMessage = "음성 녹화 시작에 실패했습니다";
            
            // HTTP 상태 코드별 에러 처리
            if (e instanceof OpenViduHttpException) {
                OpenViduHttpException httpException = (OpenViduHttpException) e;
                int statusCode = httpException.getStatus();
                
                switch (statusCode) {
                    case 501:
                        errorMessage = "OpenVidu 서버에서 녹화 기능을 지원하지 않습니다. OpenVidu Community Edition(CE)을 사용 중인 경우 녹화 기능은 Pro 버전에서만 사용 가능합니다.";
                        break;
                    case 404:
                        errorMessage = "세션을 찾을 수 없습니다. 세션이 생성되고 참가자가 연결되어 있는지 확인하세요.";
                        break;
                    case 406:
                        errorMessage = "녹화를 시작할 수 없습니다. 세션에 활성 참가자가 없거나 이미 녹화가 진행 중입니다.";
                        break;
                    case 422:
                        errorMessage = "잘못된 녹화 설정입니다. 녹화 매개변수를 확인하세요.";
                        break;
                    default:
                        errorMessage = "OpenVidu 서버 오류 (HTTP " + statusCode + ")";
                }
                
                log.warn("OpenVidu HTTP 오류 - 상태 코드: {}, 메시지: {}", statusCode, httpException.getMessage());
            }
            
            if (e.getMessage().contains("SESSION_NOT_FOUND")) {
                errorMessage = "세션을 찾을 수 없습니다. 세션이 생성되고 참가자가 연결되어 있는지 확인하세요.";
            } else if (e.getMessage().contains("NO_SESSION_ID")) {
                errorMessage = "유효하지 않은 세션 ID입니다.";
            } else if (e.getMessage().contains("RECORDING_START_ERROR")) {
                errorMessage = "녹화를 시작할 수 없습니다. 세션에 참가자가 연결되어 있는지 확인하세요.";
            }
            
            throw new RuntimeException(errorMessage + ": " + e.getMessage());
        } catch (Exception e) {
            log.error("예상치 못한 녹화 시작 오류: sessionId={}, error={}", sessionId, e.getMessage(), e);
            throw new RuntimeException("음성 녹화 시작 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public Recording stopRecording(String sessionId) {
        try {
            Recording activeRecording = activeRecordings.get(sessionId);
            if (activeRecording == null) {
                throw new IllegalStateException("진행 중인 녹화가 없습니다.");
            }

            Recording stoppedRecording = openVidu.stopRecording(activeRecording.getId());
            
            // 활성 녹화 목록에서 제거
            activeRecordings.remove(sessionId);
            
            log.info("녹화 중단 완료: sessionId={}, recordingId={}", sessionId, stoppedRecording.getId());
            
            return stoppedRecording;
            
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            log.error("녹화 중단 실패: sessionId={}, error={}", sessionId, e.getMessage());
            throw new RuntimeException("녹화 중단에 실패했습니다: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<Recording> getRecordings(String sessionId) {
        try {
            return openVidu.listRecordings().stream()
                    .filter(recording -> recording.getSessionId().equals(sessionId))
                    .toList();
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            log.error("녹화 목록 조회 실패: sessionId={}, error={}", sessionId, e.getMessage());
            throw new RuntimeException("녹화 목록 조회에 실패했습니다: " + e.getMessage());
        }
    }

    public Recording getRecording(String recordingId) {
        try {
            return openVidu.getRecording(recordingId);
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            log.error("녹화 정보 조회 실패: recordingId={}, error={}", recordingId, e.getMessage());
            throw new RuntimeException("녹화 정보 조회에 실패했습니다: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Recording getActiveRecording(String sessionId) {
        return activeRecordings.get(sessionId);
    }

    @Transactional(readOnly = true)
    public boolean isRecording(String sessionId) {
        return activeRecordings.containsKey(sessionId);
    }

    @Transactional(readOnly = true)
    public List<Session> getOpenViduActiveSessions() {
        try {
            return openVidu.getActiveSessions();
        } catch (Exception e) {
            log.error("활성 세션 목록 조회 실패: {}", e.getMessage());
            throw new RuntimeException("OpenVidu 서버 연결 실패: " + e.getMessage());
        }
    }
}