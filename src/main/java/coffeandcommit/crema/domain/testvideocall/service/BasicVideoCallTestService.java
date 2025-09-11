package coffeandcommit.crema.domain.testvideocall.service;

import coffeandcommit.crema.domain.videocall.entity.VideoSession;
import coffeandcommit.crema.domain.videocall.exception.*;
import coffeandcommit.crema.domain.videocall.repository.ParticipantRepository;
import coffeandcommit.crema.domain.videocall.repository.VideoSessionRepository;
import coffeandcommit.crema.domain.videocall.entity.Participant;

import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.openvidu.java.client.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BasicVideoCallTestService {

    /** OpenVidu 서버 도메인 */
    @Value("${openvidu.domain}")
    private String openviduDomain;

    /** OpenVidu 인증 비밀키 */
    @Value("${openvidu.secret}")
    private String openviduSecret;

    private OpenVidu openVidu;

    private final VideoSessionRepository videoSessionRepository;

    private final ParticipantRepository participantRepository;

    /**
     * 세션 ID 정규화 - 중복 prefix 문제 해결 (개선된 버전)
     * @param rawName 원본 세션 이름
     * @return 정규화된 세션 ID
     * @throws IllegalArgumentException 빈 세션 이름인 경우
     */
    private String canonicalizeSessionId(String rawName) {
        if (rawName == null) {
            throw new IllegalArgumentException("세션 이름이 null입니다");
        }
        
        String base = rawName.trim();
        
        // 반복되는 prefix들 제거 (session_, test_ 순서 무관)
        base = base.replaceAll("^(?:session_|test_)+", "");
        
        // 안전한 문자만 허용 (A-Z, a-z, 0-9, _, -)
        base = base.replaceAll("[^A-Za-z0-9_-]", "-");
        
        // 빈 문자열 및 길이 검증
        if (base.isEmpty()) {
            throw new IllegalArgumentException("세션 이름이 비어있습니다: " + rawName);
        }
        if (base.length() > 50) { // OpenVidu customSessionId 길이 제한
            base = base.substring(0, 50);
            log.warn("[CANONICALIZE] 세션 ID 길이 제한으로 인한 자른: {} -> {}", rawName, base);
        }
        
        // 정규화된 prefix 적용 (소문자로 통일)
        String result = "session_test_" + base.toLowerCase(java.util.Locale.ROOT);
        
        if (!rawName.equals(result)) {
            log.debug("[CANONICALIZE] 세션 ID 정규화: {} -> {}", rawName, result);
        }
        
        return result;
    }

    /**
     * OpenVidu 예외에서 HTTP 상태 코드 추출
     */
    private int getErrorStatus(Exception error) {
        if (error instanceof OpenViduHttpException) {
            return ((OpenViduHttpException) error).getStatus();
        }
        return -1; // 비-HTTP 오류
    }

    /**
     * 409 Conflict 오류 확인
     */
    private boolean isConflictError(int status) {
        return status == 409;
    }

    /**
     * 5xx 서버 오류 확인
     */
    private boolean isServerError(int status) {
        return status >= 500 && status < 600;
    }

    /**
     * 타임아웃 오류 확인
     */
    private boolean isTimeoutError(Exception error) {
        String message = error.getMessage();
        return message != null && (
            message.toLowerCase().contains("timeout") ||
            message.toLowerCase().contains("connection") ||
            error instanceof java.net.SocketTimeoutException
        );
    }

    /**
     * Create-First 패턴: 세션 생성 시도 후 409 충돌 시 조회
     * 글로벌 fetch() 없이 효율적으로 세션 확보
     */
    private Session createOrGetSession(String sessionId) {
        log.debug("[CREATE-OR-GET] 세션 생성/조회 시도 - sessionId: {}", sessionId);
        
        try {
            // 먼저 세션 생성 시도 (idempotent 의도)
            SessionProperties sessionProperties = new SessionProperties.Builder()
                    .customSessionId(sessionId)
                    .build();
            
            Session session = openVidu.createSession(sessionProperties);
            log.info("[CREATE-OR-GET] 새 세션 생성 성공 - sessionId: {}", sessionId);
            return session;
            
        } catch (OpenViduJavaClientException | OpenViduHttpException createError) {
            int status = getErrorStatus(createError);
            
            if (isConflictError(status)) {
                // 409 충돌: 이미 존재하는 세션, 조회 시도
                log.info("[CREATE-OR-GET] 세션이 이미 존재, 조회 시도 - sessionId: {}", sessionId);
                
                try {
                    // 기존 세션 조회
                    Session existingSession = openVidu.getActiveSession(sessionId);
                    if (existingSession != null) {
                        log.info("[CREATE-OR-GET] 기존 세션 조회 성공 - sessionId: {}", sessionId);
                        return existingSession;
                    } else {
                        // 경합 상황: 409인데 getActiveSession이 null
                        log.warn("[CREATE-OR-GET] 409 충돌이지만 세션을 찾을 수 없음, fetch 후 재시도 - sessionId: {}", sessionId);
                        
                        // fallback: 글로벌 fetch 후 재시도
                        openVidu.fetch();
                        Session refetchedSession = openVidu.getActiveSession(sessionId);
                        if (refetchedSession != null) {
                            log.info("[CREATE-OR-GET] fetch 후 세션 발견 - sessionId: {}", sessionId);
                            return refetchedSession;
                        }
                        
                        // 여전히 null이면 SessionNotFoundException
                        log.error("[CREATE-OR-GET] fetch 후에도 세션을 찾을 수 없음 - sessionId: {}", sessionId);
                        throw new SessionNotFoundException("테스트 세션 생성/조회 실패 - fetch 후에도 세션 ID: " + sessionId + "를 찾을 수 없음");
                    }
                } catch (OpenViduJavaClientException | OpenViduHttpException getError) {
                    log.error("[CREATE-OR-GET] 세션 조회 중 오류 - sessionId: {}, status: {}", 
                             sessionId, getErrorStatus(getError));
                    throw new SessionNotFoundException("테스트 세션 조회 오류 - ID: " + sessionId + ", 상태 코드: " + getErrorStatus(getError));
                }
                
            } else if (isServerError(status) || isTimeoutError(createError)) {
                // 5xx 또는 타임아웃: 재시도 가능 오류
                log.warn("[CREATE-OR-GET] 서버 오류 또는 타임아웃 - sessionId: {}, status: {}", sessionId, status);
                throw new RuntimeException("서버 일시 오류, 잠시 후 다시 시도해주세요", createError);
                
            } else {
                // 기타 4xx 오류: 클라이언트 오류
                log.error("[CREATE-OR-GET] 세션 생성 실패 - sessionId: {}, status: {}, error: {}", 
                         sessionId, status, createError.getMessage());
                throw new SessionNotFoundException("테스트 세션 생성 실패 - ID: " + sessionId + ", 상태 코드: " + status);
            }
            
        } catch (Exception unexpectedError) {
            // 예상치 못한 오류
            log.error("[CREATE-OR-GET] 예상치 못한 오류 - sessionId: {}, error: {}", 
                     sessionId, unexpectedError.getMessage());
            throw new SessionNotFoundException("테스트 세션 예상치 못한 오류 - ID: " + sessionId + ", 원인: " + unexpectedError.getMessage());
        }
    }

    //세션 연결
    @PostConstruct
    private void init() {
        long initStartTime = System.currentTimeMillis();
        log.info("[OPENVIDU-INIT] 테스트 OpenVidu 초기화 시작");
        
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
            log.info("[OPENVIDU-INIT] 테스트 OpenVidu 초기화 완료 - server: {}, 전체초기화시간: {}ms", 
                    openviduUrl, totalInitTime);
            
        } catch (Exception e) {
            long totalInitTime = System.currentTimeMillis() - initStartTime;
            log.error("[OPENVIDU-INIT] 테스트 OpenVidu 초기화 실패 - domain: {}, 전체시도시간: {}ms, " +
                     "errorType: {}, error: {}", openviduDomain, totalInitTime, 
                     e.getClass().getSimpleName(), e.getMessage(), e);
            
            // 초기화 실패는 애플리케이션 시작을 막지 않고 경고만 출력
            // 런타임에 다시 시도할 수 있도록 openVidu는 null로 유지
            this.openVidu = null;
        }
    }

    /**
    테스트용 세션 생성 후, DB에 세션 정보 저장
    @param sessionName 사용자 정의 세션 이름
     @return  생성된 VideoSession 엔티티
     @throws SessionCreationException 세션 생성 실패 시
     **/
    public VideoSession createVideoSession(String sessionName){
        long methodStartTime = System.currentTimeMillis();
        log.info("[CREATE-SESSION] 테스트 세션 생성 시작 - sessionName: {}", sessionName);
        
        try{
            String sessionId = canonicalizeSessionId(sessionName);
            log.info("[CREATE-SESSION] 세션 ID 생성 완료 - sessionName: {} -> sessionId: {}", sessionName, sessionId);

            // OpenVidu 연결 상태 확인
            if (openVidu == null) {
                log.error("[CREATE-SESSION] OpenVidu 객체가 null입니다. init() 메서드가 정상적으로 실행되지 않았을 수 있습니다.");
                throw new RuntimeException("OpenVidu 객체가 초기화되지 않았습니다.");
            }
            log.debug("[CREATE-SESSION] OpenVidu 객체 확인 완료 - server: {}", openviduDomain);

            // OpenVidu Community Edition 호환성을 위한 재시도 로직
            Session openviduSession = null;
            int maxRetries = 3;
            long sessionCreateStartTime = System.currentTimeMillis();
            
            log.info("[CREATE-SESSION] OpenVidu 세션 생성 시도 시작 - sessionId: {}, maxRetries: {}", sessionId, maxRetries);
            
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                long attemptStartTime = System.currentTimeMillis();
                
                try {
                    log.info("[CREATE-SESSION] OpenVidu 세션 생성 시도 {} 시작 - sessionId: {}", attempt, sessionId);
                    
                    SessionProperties sessionProperties = new SessionProperties.Builder()
                            .customSessionId(sessionId)
                            .build();
                    
                    log.debug("[CREATE-SESSION] SessionProperties 설정 완료 - customSessionId: {}", sessionId);
                    
                    openviduSession = openVidu.createSession(sessionProperties);
                    
                    long attemptTime = System.currentTimeMillis() - attemptStartTime;
                    log.info("[CREATE-SESSION] 테스트 OpenVidu 세션 생성 성공 (시도 {}) - sessionId: {}, 생성시간: {}ms, " +
                            "openviduSessionId: {}", attempt, sessionId, attemptTime, openviduSession.getSessionId());
                    break;
                    
                } catch (Exception sessionError) {
                    long attemptTime = System.currentTimeMillis() - attemptStartTime;
                    log.error("[CREATE-SESSION] 테스트 OpenVidu 세션 생성 실패 (시도 {}/{}) - sessionId: {}, " +
                             "시도시간: {}ms, errorType: {}, error: {}", 
                             attempt, maxRetries, sessionId, attemptTime, sessionError.getClass().getSimpleName(), 
                             sessionError.getMessage(), sessionError);
                    
                    if (attempt == maxRetries) {
                        long totalAttemptTime = System.currentTimeMillis() - sessionCreateStartTime;
                        log.error("[CREATE-SESSION] 모든 재시도 실패 - sessionId: {}, 총시도시간: {}ms", 
                                 sessionId, totalAttemptTime);
                        throw sessionError;
                    }
                    
                    // 재시도 전 잠시 대기
                    log.info("[CREATE-SESSION] 재시도 전 대기 시작 - 500ms");
                    Thread.sleep(500);
                }
            }

            if (openviduSession == null) {
                log.error("[CREATE-SESSION] OpenVidu 세션 객체가 null입니다 - sessionId: {}", sessionId);
                throw new RuntimeException("OpenVidu 세션 생성 결과가 null입니다.");
            }

            long sessionCreateTime = System.currentTimeMillis() - sessionCreateStartTime;
            log.info("[CREATE-SESSION] OpenVidu 세션 생성 전체 완료 - sessionId: {}, 총생성시간: {}ms", 
                     sessionId, sessionCreateTime);

            // DB 저장 준비
            long dbSaveStartTime = System.currentTimeMillis();
            log.info("[CREATE-SESSION] DB 세션 저장 시작 - sessionId: {}, sessionName: {}", sessionId, sessionName);
            
            VideoSession videoSession = VideoSession.builder()
                    .sessionId(sessionId)
                    .sessionName(sessionName)
                    .build();

            log.debug("[CREATE-SESSION] VideoSession 엔티티 생성 완료 - sessionId: {}, sessionName: {}, " +
                     "createdAt: {}, isActive: {}", videoSession.getSessionId(), videoSession.getSessionName(),
                     videoSession.getCreatedAt(), videoSession.getIsActive());

            VideoSession saved = videoSessionRepository.save(videoSession);
            
            long dbSaveTime = System.currentTimeMillis() - dbSaveStartTime;
            long totalTime = System.currentTimeMillis() - methodStartTime;
            
            log.info("[CREATE-SESSION] 테스트 DB에 세션 저장 완료 - sessionId: {}, DB저장시간: {}ms, " +
                    "전체처리시간: {}ms, savedId: {}", sessionId, dbSaveTime, totalTime, saved.getId());
            
            return saved;
            
        }catch (Exception e){
            long totalTime = System.currentTimeMillis() - methodStartTime;
            log.error("[CREATE-SESSION] 테스트 session create 완전 실패 - sessionName: {}, 전체처리시간: {}ms, " +
                     "errorType: {}, error: {}", sessionName, totalTime, e.getClass().getSimpleName(), 
                     e.getMessage(), e);
            
            // 원본 예외 타입이 적절한 경우 그대로 전파
            if (e instanceof SessionCreationException) {
                throw (SessionCreationException) e;
            }
            // 체크된 예외는 SessionCreationException으로 래핑
            throw new SessionCreationException("테스트 세션 생성 실패 - 이름: " + sessionName + ", 원인: " + e.getMessage());
        }
    }

    /**
     * 테스트용 세션에 참가하고, WebRTC용 토큰을 발급 (VideoSession 객체 사용)
     * @param videoSession 참가할 VideoSession 엔티티
     * @param userName     참가자 사용자명
     * @return WebRTC 연결을 위한 토큰
     */
    public String joinSession(VideoSession videoSession, String userName){
        long methodStartTime = System.currentTimeMillis();
        log.info("[JOIN-SESSION] 테스트 세션 참가 시작 - sessionId: {}, userName: {}", videoSession.getSessionId(), userName);
        
        try{
            // DB 세션 조회 생략 - 전달받은 엔티티 사용
            log.info("[JOIN-SESSION] 전달받은 세션 사용 - sessionId: {}, sessionName: {}, isActive: {}", 
                    videoSession.getSessionId(), videoSession.getSessionName(), videoSession.getIsActive());

            // OpenVidu 활성 세션 확인
            long openviduCheckStartTime = System.currentTimeMillis();
            log.info("[JOIN-SESSION] OpenVidu 활성 세션 확인 시작 - sessionId: {}", videoSession.getSessionId());
            
            if (openVidu == null) {
                log.error("[JOIN-SESSION] OpenVidu 객체가 null입니다 - sessionId: {}", videoSession.getSessionId());
                throw new RuntimeException("OpenVidu 객체가 초기화되지 않았습니다.");
            }
            
            // Create-First 패턴: 바로 세션 생성 시도 (idempotent)
            Session openviduSession = createOrGetSession(videoSession.getSessionId());
            long openviduCheckTime = System.currentTimeMillis() - openviduCheckStartTime;
            
            // DB 상태 업데이트
            videoSession.activateSession();
            videoSessionRepository.save(videoSession);
            
            log.info("[JOIN-SESSION] OpenVidu 활성 세션 확인 성공 - sessionId: {}, activeConnections: {}, " +
                    "확인시간: {}ms", videoSession.getSessionId(), openviduSession.getActiveConnections().size(), openviduCheckTime);
            
            // 연결 속성 설정
            long connectionPropsStartTime = System.currentTimeMillis();
            log.info("[JOIN-SESSION] 연결 속성 설정 시작 - userName: {}", userName);
            
            String clientData = "{\"username:\"" + userName + "\"}";
            log.debug("[JOIN-SESSION] 클라이언트 데이터 준비 완료 - data: {}", clientData);
            
            ConnectionProperties connectionProperties = new ConnectionProperties.Builder()
                    .type(ConnectionType.WEBRTC)    //저지연 WebRTC 사용
                    .data(clientData)    //연결에 대한 추가정보, json 형태로 전달
                    .role(OpenViduRole.PUBLISHER)   //Publisher : 송수신 동시에, SUBSCRIBER 수신만, MODERATOR 관리자
                    .build();
            
            long connectionPropsTime = System.currentTimeMillis() - connectionPropsStartTime;
            log.info("[JOIN-SESSION] 연결 속성 설정 완료 - type: WEBRTC, role: PUBLISHER, 설정시간: {}ms", 
                    connectionPropsTime);
            
            // OpenVidu 연결 생성
            long createConnectionStartTime = System.currentTimeMillis();
            log.info("[JOIN-SESSION] OpenVidu 연결 생성 시작 - sessionId: {}, userName: {}", videoSession.getSessionId(), userName);
            
            Connection connection;
            try {
                connection = openviduSession.createConnection(connectionProperties);
            } catch (Exception connectionError) {
                long createConnectionTime = System.currentTimeMillis() - createConnectionStartTime;
                log.error("[JOIN-SESSION] OpenVidu 연결 생성 실패 - sessionId: {}, userName: {}, " +
                         "시도시간: {}ms, errorType: {}, error: {}", 
                         videoSession.getSessionId(), userName, createConnectionTime, connectionError.getClass().getSimpleName(), 
                         connectionError.getMessage(), connectionError);
                throw connectionError;
            }
            
            long createConnectionTime = System.currentTimeMillis() - createConnectionStartTime;
            log.info("[JOIN-SESSION] OpenVidu 연결 생성 성공 - sessionId: {}, userName: {}, " +
                    "connectionId: {}, 생성시간: {}ms", videoSession.getSessionId(), userName, connection.getConnectionId(), 
                    createConnectionTime);

            // 토큰 처리
            long tokenProcessStartTime = System.currentTimeMillis();
            String originalToken = connection.getToken();
            log.info("[JOIN-SESSION] 원본 토큰 발급 완료 - connectionId: {}, 토큰길이: {}, 토큰시작: {}", 
                    connection.getConnectionId(), originalToken != null ? originalToken.length() : 0,
                    originalToken != null ? originalToken.substring(0, Math.min(20, originalToken.length())) + "..." : "null");

            String token = originalToken;
            log.debug("[OPENVIDU TEST] session connect token = {}", originalToken);

            if (originalToken.startsWith("tok_")) { //웹소켓 링크 형식 token이 전달되지 않았을 때 처리
                token = String.format("wss://"+openviduDomain+"?sessionId=%s&token=%s",
                        videoSession.getSessionId(), originalToken);
                log.info("[JOIN-SESSION] 토큰 형식 변환 완료 - 원본: tok_*, 변환: wss://형식, 최종길이: {}", 
                        token.length());
            } else {
                log.info("[JOIN-SESSION] 토큰 형식 변환 불필요 - 원본 토큰 사용");
            }
            
            long tokenProcessTime = System.currentTimeMillis() - tokenProcessStartTime;
            log.debug("[OPENVIDU TEST] connection success / ID = {}", connection.getConnectionId());
            log.info("[JOIN-SESSION] 토큰 처리 완료 - 처리시간: {}ms", tokenProcessTime);

            // 참가자 정보 DB 저장
            long participantSaveStartTime = System.currentTimeMillis();
            log.info("[JOIN-SESSION] 참가자 정보 DB 저장 시작 - connectionId: {}, userName: {}", 
                    connection.getConnectionId(), userName);
            
            Participant participant = Participant.builder()
                    .connectionId(connection.getConnectionId())
                    .token(token)
                    .username(userName)
                    .joinedAt(LocalDateTime.now())  // 참가 시간 설정
                    .isConnected(true)  // 연결 상태 설정
                    .videoSession(videoSession)
                    .member(null)  // 테스트 버전에서는 Member 설정하지 않음
                    .build();
            
            log.debug("[JOIN-SESSION] Participant 엔티티 생성 완료 - connectionId: {}, userName: {}, " +
                     "joinedAt: {}, isConnected: {}", participant.getConnectionId(), participant.getUsername(),
                     participant.getJoinedAt(), participant.getIsConnected());
            
            Participant savedParticipant = participantRepository.save(participant);
            
            long participantSaveTime = System.currentTimeMillis() - participantSaveStartTime;
            long totalTime = System.currentTimeMillis() - methodStartTime;
            
            log.info("[JOIN-SESSION] 테스트 세션 참가 완료 - sessionId: {}, userName: {}, connectionId: {}, " +
                    "participantId: {}, DB저장시간: {}ms, 전체처리시간: {}ms", 
                    videoSession.getSessionId(), userName, connection.getConnectionId(), savedParticipant.getId(),
                    participantSaveTime, totalTime);

            return token;
            
        }catch (Exception e){
            long totalTime = System.currentTimeMillis() - methodStartTime;
            log.error("[JOIN-SESSION] 테스트 join session 완전 실패 - sessionId: {}, userName: {}, " +
                     "전체처리시간: {}ms, errorType: {}, error: {}", 
                     videoSession.getSessionId(), userName, totalTime, e.getClass().getSimpleName(), e.getMessage(), e);
            
            // 원본 예외 타입이 적절한 경우 그대로 전파
            if (e instanceof SessionCreationException) {
                throw (SessionCreationException) e;
            }
            if (e instanceof SessionNotFoundException) {
                throw (SessionNotFoundException) e;
            }
            // 체크된 예외는 SessionCreationException으로 래핑
            throw new SessionCreationException("테스트 토큰 발급 실패 - 세션: " + videoSession.getSessionId() + ", 사용자: " + userName + ", 원인: " + e.getMessage());
        }
    }
    
    /**
     * 테스트용 세션에 참가하고, WebRTC용 토큰을 발급 (기존 메서드 - 호환성 유지)
     * @param sessionId 참가할 SessionId
     * @param userName  참가자 사용자명
     * @return WebRTC 연결을 위한 토큰
     */
    public String joinSession(String sessionId, String userName){
        long methodStartTime = System.currentTimeMillis();
        log.info("[JOIN-SESSION] 테스트 세션 참가 시작 - sessionId: {}, userName: {}", sessionId, userName);
        
        try{
            // DB 세션 조회
            long dbCheckStartTime = System.currentTimeMillis();
            log.info("[JOIN-SESSION] DB 세션 조회 시작 - sessionId: {}", sessionId);
            
            VideoSession videoSession = videoSessionRepository
                    .findBySessionId(sessionId)
                    .orElseThrow(SessionNotFoundException::new);
            
            long dbCheckTime = System.currentTimeMillis() - dbCheckStartTime;
            log.info("[JOIN-SESSION] DB 세션 조회 성공 - sessionId: {}, sessionName: {}, isActive: {}, " +
                    "createdAt: {}, 조회시간: {}ms", sessionId, videoSession.getSessionName(), 
                    videoSession.getIsActive(), videoSession.getCreatedAt(), dbCheckTime);

            // OpenVidu 활성 세션 확인
            long openviduCheckStartTime = System.currentTimeMillis();
            log.info("[JOIN-SESSION] OpenVidu 활성 세션 확인 시작 - sessionId: {}", sessionId);
            
            if (openVidu == null) {
                log.error("[JOIN-SESSION] OpenVidu 객체가 null입니다 - sessionId: {}", sessionId);
                throw new RuntimeException("OpenVidu 객체가 초기화되지 않았습니다.");
            }
            
            // Create-First 패턴: 바로 세션 생성 시도 (idempotent)
            Session openviduSession = createOrGetSession(sessionId);
            long openviduCheckTime = System.currentTimeMillis() - openviduCheckStartTime;
            
            // DB 상태 업데이트
            videoSession.activateSession();
            videoSessionRepository.save(videoSession);
            
            log.info("[JOIN-SESSION] OpenVidu 활성 세션 확인 성공 - sessionId: {}, activeConnections: {}, " +
                    "확인시간: {}ms", sessionId, openviduSession.getActiveConnections().size(), openviduCheckTime);
            
            // 연결 속성 설정
            long connectionPropsStartTime = System.currentTimeMillis();
            log.info("[JOIN-SESSION] 연결 속성 설정 시작 - userName: {}", userName);
            
            String clientData = "{\"username\":\"" + userName + "\"}";
            log.debug("[JOIN-SESSION] 클라이언트 데이터 준비 완료 - data: {}", clientData);
            
            ConnectionProperties connectionProperties = new ConnectionProperties.Builder()
                    .type(ConnectionType.WEBRTC)    //저지연 WebRTC 사용
                    .data(clientData)    //연결에 대한 추가정보, json 형태로 전달
                    .role(OpenViduRole.PUBLISHER)   //Publisher : 송수신 동시에, SUBSCRIBER 수신만, MODERATOR 관리자
                    .build();
            
            long connectionPropsTime = System.currentTimeMillis() - connectionPropsStartTime;
            log.info("[JOIN-SESSION] 연결 속성 설정 완료 - type: WEBRTC, role: PUBLISHER, 설정시간: {}ms", 
                    connectionPropsTime);
            
            // OpenVidu 연결 생성
            long createConnectionStartTime = System.currentTimeMillis();
            log.info("[JOIN-SESSION] OpenVidu 연결 생성 시작 - sessionId: {}, userName: {}", sessionId, userName);
            
            Connection connection;
            try {
                connection = openviduSession.createConnection(connectionProperties);
            } catch (Exception connectionError) {
                long createConnectionTime = System.currentTimeMillis() - createConnectionStartTime;
                log.error("[JOIN-SESSION] OpenVidu 연결 생성 실패 - sessionId: {}, userName: {}, " +
                         "시도시간: {}ms, errorType: {}, error: {}", 
                         sessionId, userName, createConnectionTime, connectionError.getClass().getSimpleName(), 
                         connectionError.getMessage(), connectionError);
                throw connectionError;
            }
            
            long createConnectionTime = System.currentTimeMillis() - createConnectionStartTime;
            log.info("[JOIN-SESSION] OpenVidu 연결 생성 성공 - sessionId: {}, userName: {}, " +
                    "connectionId: {}, 생성시간: {}ms", sessionId, userName, connection.getConnectionId(), 
                    createConnectionTime);

            // 토큰 처리
            long tokenProcessStartTime = System.currentTimeMillis();
            String originalToken = connection.getToken();
            log.info("[JOIN-SESSION] 원본 토큰 발급 완료 - connectionId: {}, 토큰길이: {}, 토큰시작: {}", 
                    connection.getConnectionId(), originalToken != null ? originalToken.length() : 0,
                    originalToken != null ? originalToken.substring(0, Math.min(20, originalToken.length())) + "..." : "null");

            String token = originalToken;
            log.debug("[OPENVIDU TEST] session connect token = {}", originalToken);

            if (originalToken.startsWith("tok_")) { //웹소켓 링크 형식 token이 전달되지 않았을 때 처리
                token = String.format("wss://"+openviduDomain+"?sessionId=%s&token=%s",
                        sessionId, originalToken);
                log.info("[JOIN-SESSION] 토큰 형식 변환 완료 - 원본: tok_*, 변환: wss://형식, 최종길이: {}", 
                        token.length());
            } else {
                log.info("[JOIN-SESSION] 토큰 형식 변환 불필요 - 원본 토큰 사용");
            }
            
            long tokenProcessTime = System.currentTimeMillis() - tokenProcessStartTime;
            log.debug("[OPENVIDU TEST] connection success / ID = {}", connection.getConnectionId());
            log.info("[JOIN-SESSION] 토큰 처리 완료 - 처리시간: {}ms", tokenProcessTime);

            // 참가자 정보 DB 저장
            long participantSaveStartTime = System.currentTimeMillis();
            log.info("[JOIN-SESSION] 참가자 정보 DB 저장 시작 - connectionId: {}, userName: {}", 
                    connection.getConnectionId(), userName);
            
            Participant participant = Participant.builder()
                    .connectionId(connection.getConnectionId())
                    .token(token)
                    .username(userName)
                    .joinedAt(LocalDateTime.now())  // 참가 시간 설정
                    .isConnected(true)  // 연결 상태 설정
                    .videoSession(videoSession)
                    .member(null)  // 테스트 버전에서는 Member 설정하지 않음
                    .build();
            
            log.debug("[JOIN-SESSION] Participant 엔티티 생성 완료 - connectionId: {}, userName: {}, " +
                     "joinedAt: {}, isConnected: {}", participant.getConnectionId(), participant.getUsername(),
                     participant.getJoinedAt(), participant.getIsConnected());
            
            Participant savedParticipant = participantRepository.save(participant);
            
            long participantSaveTime = System.currentTimeMillis() - participantSaveStartTime;
            long totalTime = System.currentTimeMillis() - methodStartTime;
            
            log.info("[JOIN-SESSION] 테스트 세션 참가 완료 - sessionId: {}, userName: {}, connectionId: {}, " +
                    "participantId: {}, DB저장시간: {}ms, 전체처리시간: {}ms", 
                    sessionId, userName, connection.getConnectionId(), savedParticipant.getId(),
                    participantSaveTime, totalTime);

            return token;
            
        }catch (Exception e){
            long totalTime = System.currentTimeMillis() - methodStartTime;
            log.error("[JOIN-SESSION] 테스트 join session 완전 실패 - sessionId: {}, userName: {}, " +
                     "전체처리시간: {}ms, errorType: {}, error: {}", 
                     sessionId, userName, totalTime, e.getClass().getSimpleName(), e.getMessage(), e);
            
            // 원본 예외 타입이 적절한 경우 그대로 전파
            if (e instanceof SessionCreationException) {
                throw (SessionCreationException) e;
            }
            if (e instanceof SessionNotFoundException) {
                throw (SessionNotFoundException) e;
            }
            // 체크된 예외는 SessionCreationException으로 래핑
            throw new SessionCreationException("테스트 토큰 발급 실패(호환) - 세션: " + sessionId + ", 사용자: " + userName + ", 원인: " + e.getMessage());
        }
    }

    //테스트용 세션 떠나기
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
            log.error("테스트 leave session failed {}", e.getMessage());
        }
    }

    //테스트용 음성 녹화 시작
    public Recording startAudioRecording(String sessionId){
        try{
            VideoSession videoSession = videoSessionRepository.findBySessionIdAndIsActiveTrue(sessionId)
                    .orElseThrow(() -> new SessionNotFoundException("테스트 녹화용 활성 세션 ID: " + sessionId + "를 찾을 수 없습니다"));

            Session openviduSession = openVidu.getActiveSession(sessionId);
            if(openviduSession == null){
                throw new SessionNotFoundException("테스트 녹화용 OpenVidu 세션 ID: " + sessionId + "가 없습니다");
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
                throw new RecordingAlreadyStartedException("테스트 세션 " + sessionId + "에서 이미 녹화가 진행 중입니다");
            }

            RecordingProperties recordingProperties = new RecordingProperties.Builder()
                    .name("test_audio_record_" + videoSession.getSessionName())
                    .outputMode(Recording.OutputMode.COMPOSED)  //Composed : 모든 음성을 하나로, INDIVIDUAL : 참가자 각각 녹음
                    .hasAudio(true)
                    .hasVideo(false)
                    .build();

            try{
                Recording recording = openVidu.startRecording(sessionId, recordingProperties);
                log.info("[OPENVIDU TEST] 녹화 시작: sessionId={}, recordingId={}", sessionId, recording.getId());
                return recording;
            }catch (Exception e){
                log.error("[OPENVIDU TEST] session {} / recording failed {}",sessionId,  e.getMessage());
                throw new RecordingFailedException("테스트 세션 " + sessionId + " 녹화 시작 실패: " + e.getMessage());
            }
        }catch (Exception e){
            log.error("[OPENVIDU TEST] session {} / recording failed in outside {}",sessionId,  e.getMessage());
            throw new RecordingFailedException("테스트 세션 " + sessionId + " 녹화 전체 실패: " + e.getMessage());
        }
    }

    public void endSession(String sessionId) {
        try {
            VideoSession videoSession = videoSessionRepository
                    .findBySessionId(sessionId)
                    .orElseThrow(SessionNotFoundException::new);

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
            
            log.info("[OPENVIDU TEST] 세션 종료 완료: sessionId={}", sessionId);

        } catch (Exception e) {
            log.error("테스트 세션 종료 실패: {}", e.getMessage());
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
            
            log.info("[OPENVIDU TEST] 녹화 중단 완료: sessionId={}, recordingId={}", sessionId, stoppedRecording.getId());
            
            return stoppedRecording;
            
        } catch (Exception e) {
            log.error("테스트 녹화 중단 실패: sessionId={}, error={}", sessionId, e.getMessage());
            throw new RecordingFailedException("테스트 세션 " + sessionId + " 녹화 중단 실패: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<Recording> getRecordings(String sessionId) {
        try {
            return openVidu.listRecordings().stream()
                    .filter(recording -> recording.getSessionId().equals(sessionId))
                    .toList();
        } catch (Exception e) {
            log.error("테스트 녹화 목록 조회 실패: sessionId={}, error={}", sessionId, e.getMessage());
            throw new RecordingFailedException("테스트 세션 " + sessionId + " 녹화 목록 조회 실패: " + e.getMessage());
        }
    }

    public Recording getRecording(String recordingId) {
        try {
            return openVidu.getRecording(recordingId);
        } catch (Exception e) {
            log.error("테스트 녹화 정보 조회 실패: recordingId={}, error={}", recordingId, e.getMessage());
            throw new RecordingFailedException("테스트 녹화 ID " + recordingId + " 정보 조회 실패: " + e.getMessage());
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
            log.error("테스트 활성 녹화 조회 실패: sessionId={}, error={}", sessionId, e.getMessage());
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
            log.error("테스트 녹화 상태 확인 실패: sessionId={}, error={}", sessionId, e.getMessage());
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<Session> getOpenViduActiveSessions() {
        try {
            return openVidu.getActiveSessions();
        } catch (Exception e) {
            log.error("테스트 활성 세션 목록 조회 실패: {}", e.getMessage());
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
                throw new SessionNotFoundException("테스트 화면 공유용 OpenVidu 세션 ID: " + sessionId + "가 없습니다");
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
            
            log.info("[OPENVIDU TEST] 화면공유 시작 성공: sessionId={}, connectionId={}", sessionId, connectionId);

        } catch (Exception e) {
            log.error("테스트 화면공유 시작 실패: sessionId={}, connectionId={}, error={}", 
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
            
            log.info("[OPENVIDU TEST] 화면공유 중지 성공: sessionId={}, connectionId={}", sessionId, connectionId);

        } catch (Exception e) {
            log.error("테스트 화면공유 중지 실패: sessionId={}, connectionId={}, error={}", 
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
            log.error("테스트 화면공유 상태 확인 실패: sessionId={}, error={}", sessionId, e.getMessage());
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
            log.error("테스트 화면공유 참가자 조회 실패: sessionId={}, error={}", sessionId, e.getMessage());
            return null;
        }
    }

}