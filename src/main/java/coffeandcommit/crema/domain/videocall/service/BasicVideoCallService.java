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
import io.openvidu.java.client.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class BasicVideoCallService {

    /** OpenVidu 서버 URL (내부 통신용) */
    @Value("${openvidu.url}")
    private String openviduUrl;

    /** OpenVidu 인증 비밀키 */
    @Value("${openvidu.secret}")
    private String openviduSecret;

    /** OpenVidu domain */
    @Value("${openvidu.domain}")
    private String openviduDomain;

    private OpenVidu openVidu;

    private final VideoSessionRepository videoSessionRepository;

    private final ParticipantRepository participantRepository;

    private final MemberRepository memberRepository;

    /** 활성 녹화 세션 관리 (sessionId -> Recording) */
    private final Map<String, Recording> activeRecordings = new ConcurrentHashMap<>();

    /** 화면공유 상태 추적 (sessionId -> connectionId) */
    private final Map<String, String> activeScreenShares = new ConcurrentHashMap<>();

    //세션 연결
    @PostConstruct
    private void init() {
        this.openVidu = new OpenVidu(openviduUrl, openviduSecret);
        log.info("Openvidu server connected");
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
                    .orElseThrow(SessionNotFound::new);

            Session openviduSession = openVidu.getActiveSession(sessionId);
            if(openviduSession == null){
                throw new SessionNotFound();
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
                    .orElseThrow(SessionNotFound::new);
            videoSession.endSession();
            videoSessionRepository.save(videoSession);

            List<Participant> connectedParticipants = participantRepository.findByVideoSessionAndIsConnectedTrue(videoSession);
            connectedParticipants.forEach(Participant::leaveSession);

            Session openviduSession = openVidu.getActiveSession(sessionId);
            if(openviduSession != null){
                openviduSession.close();
            }

        }catch (Exception e){
            log.error("leave session failed {}", e.getMessage());
        }
    }

    //음성 녹화 시작
    public Recording startAudioRecording(String sessionId){
        try{
            VideoSession videoSession = videoSessionRepository.findBySessionIdAndIsActiveTrue(sessionId)
                    .orElseThrow(SessionNotFound::new);

            Session openviduSession = openVidu.getActiveSession(sessionId);
            if(openviduSession == null){
                throw new SessionNotFound();
            }

            List<Participant> connectedParticipants = participantRepository.findByVideoSessionAndIsConnectedTrue(videoSession);
            if(connectedParticipants.isEmpty()){
                throw new ParticipantNotFound();
            }

            if(activeRecordings.containsKey(sessionId)){
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
                activeRecordings.put(sessionId, recording);
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

}
