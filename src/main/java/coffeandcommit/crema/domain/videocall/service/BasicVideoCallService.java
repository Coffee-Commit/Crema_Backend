package coffeandcommit.crema.domain.videocall.service;

import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import coffeandcommit.crema.domain.videocall.entity.VideoSession;
import coffeandcommit.crema.domain.videocall.exception.*;
import coffeandcommit.crema.domain.videocall.repository.ParticipantRepository;
import coffeandcommit.crema.domain.videocall.repository.VideoSessionRepository;
import coffeandcommit.crema.domain.videocall.entity.Participant;

import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.openvidu.java.client.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BasicVideoCallService {

    /** OpenVidu 서버 도메인 */
    @Value("${openvidu.domain}")
    private String openviduDomain;

    /** OpenVidu 인증 비밀키 */
    @Value("${openvidu.secret}")
    private String openviduSecret;

    private OpenVidu openVidu;

    private final VideoSessionRepository videoSessionRepository;

    private final ParticipantRepository participantRepository;

    private final MemberRepository memberRepository;


    //세션 연결
    @PostConstruct
    private void init() {
        long initStartTime = System.currentTimeMillis();
        log.info("[OPENVIDU-INIT] OpenVidu 초기화 시작 (운영)");
        
        try {
            // 환경 변수 확인
            log.info("[OPENVIDU-INIT] 환경 설정 확인 - domain: {}, secret: {}", 
                    openviduDomain, openviduSecret != null ? "***설정됨***" : "null");
            
            if (openviduDomain == null || openviduDomain.trim().isEmpty()) {
                log.error("[OPENVIDU-INIT] OpenVidu 도메인이 설정되지 않았습니다 - domain: {}", openviduDomain);
                throw new IllegalStateException("OpenVidu 도메인이 설정되지 않았습니다.");
            }
            
            if (openviduSecret == null || openviduSecret.trim().isEmpty()) {
                log.error("[OPENVIDU-INIT] OpenVidu 시크릿이 설정되지 않았습니다 - secret: {}", 
                         openviduSecret != null ? "빈문자열" : "null");
                throw new IllegalStateException("OpenVidu 시크릿이 설정되지 않았습니다.");
            }
            
            String openviduUrl = "https://" + openviduDomain;
            log.info("[OPENVIDU-INIT] OpenVidu URL 구성 완료 - url: {}", openviduUrl);
            
            // OpenVidu 객체 생성
            long openviduCreateStartTime = System.currentTimeMillis();
            log.info("[OPENVIDU-INIT] OpenVidu 객체 생성 시작 - url: {}", openviduUrl);
            
            try {
                this.openVidu = new OpenVidu(openviduUrl, openviduSecret);
            } catch (Exception openViduError) {
                long openviduCreateTime = System.currentTimeMillis() - openviduCreateStartTime;
                log.error("[OPENVIDU-INIT] OpenVidu 객체 생성 실패 - url: {}, 시도시간: {}ms, " +
                         "errorType: {}, error: {}", openviduUrl, openviduCreateTime, 
                         openViduError.getClass().getSimpleName(), openViduError.getMessage(), openViduError);
                throw openViduError;
            }
            
            long openviduCreateTime = System.currentTimeMillis() - openviduCreateStartTime;
            log.info("[OPENVIDU-INIT] OpenVidu 객체 생성 성공 - 생성시간: {}ms", openviduCreateTime);
            
            // 연결 테스트 (선택적)
            long connectionTestStartTime = System.currentTimeMillis();
            log.info("[OPENVIDU-INIT] OpenVidu 서버 연결 상태 테스트 시작");
            
            try {
                // getActiveSessions 호출로 서버 연결 상태 확인
                this.openVidu.getActiveSessions();
                long connectionTestTime = System.currentTimeMillis() - connectionTestStartTime;
                log.info("[OPENVIDU-INIT] OpenVidu 서버 연결 상태 테스트 성공 - 테스트시간: {}ms", connectionTestTime);
            } catch (Exception connectionTestError) {
                long connectionTestTime = System.currentTimeMillis() - connectionTestStartTime;
                log.warn("[OPENVIDU-INIT] OpenVidu 서버 연결 상태 테스트 실패 (객체는 생성됨) - " +
                        "테스트시간: {}ms, errorType: {}, error: {}", connectionTestTime, 
                        connectionTestError.getClass().getSimpleName(), connectionTestError.getMessage());
                // 연결 테스트 실패는 경고만 하고 초기화는 계속 진행
            }
            
            long totalInitTime = System.currentTimeMillis() - initStartTime;
            log.info("[OPENVIDU-INIT] OpenVidu 초기화 완료 (운영) - server: {}, 전체초기화시간: {}ms", 
                    openviduUrl, totalInitTime);
            
        } catch (Exception e) {
            long totalInitTime = System.currentTimeMillis() - initStartTime;
            log.error("[OPENVIDU-INIT] OpenVidu 초기화 실패 (운영) - domain: {}, 전체시도시간: {}ms, " +
                     "errorType: {}, error: {}", openviduDomain, totalInitTime, 
                     e.getClass().getSimpleName(), e.getMessage(), e);
            
            // 초기화 실패는 애플리케이션 시작을 막지 않고 경고만 출력
            // 런타임에 다시 시도할 수 있도록 openVidu는 null로 유지
            this.openVidu = null;
        }
    }

    /**
    세션 생성 후, DB에 세션 정보 저장
    @param sessionName 사용자 정의 세션 이름
     @return  생성된 VideoSession 엔티티
     @throws SessionCreationException 세션 생성 실패 시
     **/
    public VideoSession createVideoSession(String sessionName){
        try{
            String sessionId = "session_" + sessionName;

            SessionProperties sessionProperties = new SessionProperties.Builder()
                    .customSessionId(sessionId)
                    .mediaMode(MediaMode.ROUTED)   //ROUTED -> 서버 경유 연결(안정적), RELAYED -> P2P 연결(속도 지향)
                    .recordingMode(RecordingMode.MANUAL)    //MANUAL -> 필요할때만 녹화
                    .build();

            Session openviduSession = openVidu.createSession(sessionProperties);

            VideoSession videoSession = VideoSession.builder()
                    .sessionId(sessionId)
                    .sessionName(sessionName)
                    .build();

            return videoSessionRepository.save(videoSession);
        }catch (Exception e){
            log.error("session create failed {}", e.getMessage());
            throw new SessionCreationException();
        }
    }

    /**
     * 세션에 참가하고, WebRTC용 토큰을 발급
     * @param sessionId 참가할 SessionId
     * @param userName  참가자 사용자명
     * @return WebRTC 연결을 위한 토큰
     */
    public String joinSession(String sessionId, String userName){
        try{
            Optional<Member> byNicknameAndIsDeletedFalse = memberRepository.findByNicknameAndIsDeletedFalse(userName);
            //추후 수정 일단 없으면 에러 발생하게
            if(byNicknameAndIsDeletedFalse.isEmpty()){
                throw new RuntimeException();
            }

            VideoSession videoSession = videoSessionRepository
                    .findBySessionId(sessionId)
                    .orElseThrow(SessionNotFoundException::new);

            Session openviduSession = openVidu.getActiveSession(sessionId);
            if(openviduSession == null){
                throw new SessionNotFoundException();
            }
            ConnectionProperties connectionProperties = new ConnectionProperties.Builder()
                    .type(ConnectionType.WEBRTC)    //저지연 WebRTC 사용
                    .data("{\"username\":\"" + userName + "\"}")    //연결에 대한 추가정보, json 형태로 전달
                    .role(OpenViduRole.PUBLISHER)   //Publisher : 송수신 동시에, SUBSCRIBER 수신만, MODERATOR 관리자
                    .build();
            Connection connection = openviduSession.createConnection(connectionProperties);
            String originalToken = connection.getToken();

            String token = originalToken;
            log.debug("[OPENVIDU] session connect token = {}", originalToken);

            if (originalToken.startsWith("tok_")) { //웹소켓 링크 형식 token이 전달되지 않았을 때 처리
                token = String.format("wss://"+openviduDomain+"?sessionId=%s&token=%s",
                        sessionId, originalToken);
            }
            log.debug("[OPENVIDU] connection success / ID = {}", connection.getConnectionId());

            //참가자 정보 저장
            Participant participant = Participant.builder()
                    .connectionId(connection.getConnectionId())
                    .token(token)
                    .username(userName)
                    .videoSession(videoSession)
                    .member(byNicknameAndIsDeletedFalse.get())
                    .build();
            participantRepository.save(participant);

            return token;
        }catch (Exception e){
            log.error("join session failed {}", e.getMessage());
            throw new SessionCreationException();
        }
    }

    //세션 떠나기
    public void leaveSession(String sessionId, String connectionId){
        try{
            VideoSession videoSession = videoSessionRepository.findBySessionId(sessionId)
                    .orElseThrow(SessionNotFoundException::new);

            // 특정 참가자만 세션에서 나가기
            Participant participant = participantRepository.findByConnectionId(connectionId)
                    .orElseThrow(ParticipantNotFound::new);
            participant.leaveSession();
            participantRepository.save(participant);

            // 모든 참가자가 나갔는지 확인
            List<Participant> remainingParticipants = participantRepository.findByVideoSessionAndIsConnectedTrue(videoSession);
            
            // 마지막 참가자가 나간 경우에만 세션 종료
            if(remainingParticipants.isEmpty()) {
                videoSession.endSession();
                videoSessionRepository.save(videoSession);

                Session openviduSession = openVidu.getActiveSession(sessionId);
                if(openviduSession != null){
                    openviduSession.close();
                }
            }

        }catch (Exception e){
            log.error("leave session failed {}", e.getMessage());
        }
    }

    //음성 녹화 시작
    public Recording startAudioRecording(String sessionId){
        try{
            VideoSession videoSession = videoSessionRepository.findBySessionIdAndIsActiveTrue(sessionId)
                    .orElseThrow(SessionNotFoundException::new);

            Session openviduSession = openVidu.getActiveSession(sessionId);
            if(openviduSession == null){
                throw new SessionNotFoundException();
            }

            List<Participant> connectedParticipants = participantRepository.findByVideoSessionAndIsConnectedTrue(videoSession);
            if(connectedParticipants.isEmpty()){
                throw new ParticipantNotFound();
            }

            List<Recording> activeRecordings = openVidu.listRecordings().stream()
                    .filter(recording -> recording.getSessionId().equals(sessionId) && 
                            recording.getStatus() == Recording.Status.started)
                    .toList();
            
            if(!activeRecordings.isEmpty()){
                throw new RecordingAlreadyStartedException();
            }

            RecordingProperties recordingProperties = new RecordingProperties.Builder()
                    .name("audio_record_" + videoSession.getSessionName())
                    .outputMode(Recording.OutputMode.COMPOSED)  //Composed : 모든 음성을 하나로, INDIVIDUAL : 참가자 각각 녹음
                    .hasAudio(true)
                    .hasVideo(false)
                    .build();

            try{
                Recording recording = openVidu.startRecording(sessionId, recordingProperties);
                return recording;
            }catch (Exception e){
                log.error("[OPENVIDU] session {} / recording failed {}",sessionId,  e.getMessage());
                throw new RecordingFailedException();
            }
        }catch (Exception e){
            log.error("[OPENVIDU] session {} / recording failed in outside {}",sessionId,  e.getMessage());
            throw new RecordingFailedException();
        }
    }

    public void endSession(String sessionId) {
    try {
        VideoSession videoSession = videoSessionRepository
                .findBySessionId(sessionId)
                .orElseThrow(SessionNotFoundException::new);

        // 세션 종료 전 채팅 기록 자동 저장 시도
        try {
            // 현재는 자동 저장 기능을 비활성화 (프론트엔드에서 명시적으로 저장하는 방식 사용)
            log.info("세션 종료: sessionId={}, 채팅 기록은 별도 API로 저장됩니다", sessionId);
        } catch (Exception chatSaveException) {
            log.error("채팅 기록 자동 저장 실패: sessionId={}, error={}", 
                     sessionId, chatSaveException.getMessage());
            // 채팅 저장 실패가 세션 종료를 막지 않도록 함
        }

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

        log.info("세션 종료 완료: sessionId={}", sessionId);

    } catch (Exception e) {
        log.error("세션 종료 실패: sessionId={}, error={}", sessionId, e.getMessage());
    }
}

    @Transactional(readOnly = true)
    public VideoSession getSession(String sessionId) {
        return videoSessionRepository
                .findBySessionId(sessionId)
                .orElseThrow(SessionNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<Participant> getActiveParticipants(String sessionId) {
        VideoSession videoSession = getSession(sessionId);
        return participantRepository.findByVideoSessionAndIsConnectedTrue(videoSession);
    }

    public Recording stopRecording(String sessionId) {
        try {
            List<Recording> activeRecordings = openVidu.listRecordings().stream()
                    .filter(recording -> recording.getSessionId().equals(sessionId) && 
                            recording.getStatus() == Recording.Status.started)
                    .toList();
            
            if (activeRecordings.isEmpty()) {
                throw new IllegalStateException("진행 중인 녹화가 없습니다.");
            }

            Recording activeRecording = activeRecordings.get(0);
            Recording stoppedRecording = openVidu.stopRecording(activeRecording.getId());
            
            log.info("녹화 중단 완료: sessionId={}, recordingId={}", sessionId, stoppedRecording.getId());
            
            return stoppedRecording;
            
        } catch (Exception e) {
            log.error("녹화 중단 실패: sessionId={}, error={}", sessionId, e.getMessage());
            throw new RecordingFailedException();
        }
    }

    @Transactional(readOnly = true)
    public List<Recording> getRecordings(String sessionId) {
        try {
            return openVidu.listRecordings().stream()
                    .filter(recording -> recording.getSessionId().equals(sessionId))
                    .toList();
        } catch (Exception e) {
            log.error("녹화 목록 조회 실패: sessionId={}, error={}", sessionId, e.getMessage());
            throw new RecordingFailedException();
        }
    }

    public Recording getRecording(String recordingId) {
        try {
            return openVidu.getRecording(recordingId);
        } catch (Exception e) {
            log.error("녹화 정보 조회 실패: recordingId={}, error={}", recordingId, e.getMessage());
            throw new RecordingFailedException();
        }
    }

    @Transactional(readOnly = true)
    public Recording getActiveRecording(String sessionId) {
        try {
            return openVidu.listRecordings().stream()
                    .filter(recording -> recording.getSessionId().equals(sessionId) && 
                            recording.getStatus() == Recording.Status.started)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("활성 녹화 조회 실패: sessionId={}, error={}", sessionId, e.getMessage());
            return null;
        }
    }

    @Transactional(readOnly = true)
    public boolean isRecording(String sessionId) {
        try {
            return openVidu.listRecordings().stream()
                    .anyMatch(recording -> recording.getSessionId().equals(sessionId) && 
                            recording.getStatus() == Recording.Status.started);
        } catch (Exception e) {
            log.error("녹화 상태 확인 실패: sessionId={}, error={}", sessionId, e.getMessage());
            return false;
        }
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

    public void startScreenShare(String sessionId, String connectionId) {
        try {
            VideoSession videoSession = videoSessionRepository
                    .findBySessionIdAndIsActiveTrue(sessionId)
                    .orElseThrow(SessionNotFoundException::new);

            Session openviduSession = openVidu.getActiveSession(sessionId);
            if (openviduSession == null) {
                throw new SessionNotFoundException();
            }

            boolean isAlreadySharing = openviduSession.getActiveConnections().stream()
                    .anyMatch(connection -> {
                        try {
                            String data = connection.getClientData();
                            return data != null && data.contains("\"screenSharing\":true");
                        } catch (Exception e) {
                            return false;
                        }
                    });

            if (isAlreadySharing) {
                throw new IllegalStateException("다른 참가자가 이미 화면공유 중입니다.");
            }

            Participant participant = participantRepository
                    .findByConnectionId(connectionId)
                    .orElseThrow(ParticipantNotFound::new);

            if (!participant.getIsConnected()) {
                throw new IllegalStateException("연결되지 않은 참가자는 화면공유를 시작할 수 없습니다.");
            }
            
            log.info("화면공유 시작 성공: sessionId={}, connectionId={}", sessionId, connectionId);

        } catch (Exception e) {
            log.error("화면공유 시작 실패: sessionId={}, connectionId={}, error={}", 
                     sessionId, connectionId, e.getMessage());
            throw new RuntimeException("화면공유 시작에 실패했습니다: " + e.getMessage());
        }
    }

    public void stopScreenShare(String sessionId, String connectionId) {
        try {
            Session openviduSession = openVidu.getActiveSession(sessionId);
            if (openviduSession == null) {
                log.warn("진행 중인 세션이 없습니다: sessionId={}", sessionId);
                return;
            }

            boolean isConnectionSharing = openviduSession.getActiveConnections().stream()
                    .anyMatch(connection -> {
                        try {
                            return connection.getConnectionId().equals(connectionId) &&
                                   connection.getClientData() != null &&
                                   connection.getClientData().contains("\"screenSharing\":true");
                        } catch (Exception e) {
                            return false;
                        }
                    });

            if (!isConnectionSharing) {
                log.warn("해당 참가자가 화면공유 중이 아닙니다: sessionId={}, connectionId={}", sessionId, connectionId);
                return;
            }
            
            log.info("화면공유 중지 성공: sessionId={}, connectionId={}", sessionId, connectionId);

        } catch (Exception e) {
            log.error("화면공유 중지 실패: sessionId={}, connectionId={}, error={}", 
                     sessionId, connectionId, e.getMessage());
            throw new RuntimeException("화면공유 중지에 실패했습니다: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public boolean isScreenSharing(String sessionId) {
        try {
            Session openviduSession = openVidu.getActiveSession(sessionId);
            if (openviduSession == null) {
                return false;
            }

            return openviduSession.getActiveConnections().stream()
                    .anyMatch(connection -> {
                        try {
                            String data = connection.getClientData();
                            return data != null && data.contains("\"screenSharing\":true");
                        } catch (Exception e) {
                            return false;
                        }
                    });
        } catch (Exception e) {
            log.error("화면공유 상태 확인 실패: sessionId={}, error={}", sessionId, e.getMessage());
            return false;
        }
    }

    @Transactional(readOnly = true)
    public String getCurrentScreenSharingConnectionId(String sessionId) {
        try {
            Session openviduSession = openVidu.getActiveSession(sessionId);
            if (openviduSession == null) {
                return null;
            }

            return openviduSession.getActiveConnections().stream()
                    .filter(connection -> {
                        try {
                            String data = connection.getClientData();
                            return data != null && data.contains("\"screenSharing\":true");
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .map(Connection::getConnectionId)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("화면공유 참가자 조회 실패: sessionId={}, error={}", sessionId, e.getMessage());
            return null;
        }
    }

}
