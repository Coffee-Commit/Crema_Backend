package coffeandcommit.crema.domain.videocall.service;

import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import coffeandcommit.crema.domain.videocall.entity.VideoSession;
import coffeandcommit.crema.domain.videocall.exception.*;
import coffeandcommit.crema.domain.videocall.repository.ParticipantRepository;
import coffeandcommit.crema.domain.videocall.repository.VideoSessionRepository;
import coffeandcommit.crema.domain.videocall.entity.Participant;
import coffeandcommit.crema.domain.videocall.util.NetworkDiagnostics;

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
    private final NetworkDiagnostics networkDiagnostics;


    //세션 연결
    @PostConstruct
    private void init() {
        long initStartTime = System.currentTimeMillis();
        log.info("[OPENVIDU-INIT] ========== OpenVidu 초기화 시작 (운영) ==========");
        
        try {
            // 네트워크 환경 정보 출력
            log.info("[OPENVIDU-INIT] 시스템 네트워크 환경 정보 수집 시작");
            networkDiagnostics.logNetworkEnvironment();
            
            // 환경 변수 상세 확인
            log.info("[OPENVIDU-INIT] ========== 환경 설정 검증 ==========");
            log.info("[OPENVIDU-INIT] OpenVidu 설정 정보:");
            log.info("[OPENVIDU-INIT]   - Domain: {}", openviduDomain);
            log.info("[OPENVIDU-INIT]   - Domain Length: {}", openviduDomain != null ? openviduDomain.length() : 0);
            log.info("[OPENVIDU-INIT]   - Domain Trimmed: '{}'", openviduDomain != null ? openviduDomain.trim() : "null");
            log.info("[OPENVIDU-INIT]   - Secret Present: {}", openviduSecret != null);
            log.info("[OPENVIDU-INIT]   - Secret Length: {}", openviduSecret != null ? openviduSecret.length() : 0);
            log.info("[OPENVIDU-INIT]   - Secret Trimmed Length: {}", 
                    openviduSecret != null ? openviduSecret.trim().length() : 0);
            
            if (openviduDomain == null || openviduDomain.trim().isEmpty()) {
                log.info("[OPENVIDU-INIT] ❌ OpenVidu 도메인이 설정되지 않았습니다:");
                log.info("[OPENVIDU-INIT]   - Raw Value: '{}'", openviduDomain);
                log.info("[OPENVIDU-INIT]   - 확인사항: application.yml의 openvidu.domain 또는 OPENVIDU_DOMAIN 환경변수");
                throw new IllegalStateException("OpenVidu 도메인이 설정되지 않았습니다.");
            }
            
            if (openviduSecret == null || openviduSecret.trim().isEmpty()) {
                log.info("[OPENVIDU-INIT] ❌ OpenVidu 시크릿이 설정되지 않았습니다:");
                log.info("[OPENVIDU-INIT]   - Raw Value: '{}'", openviduSecret);
                log.info("[OPENVIDU-INIT]   - 확인사항: application.yml의 openvidu.secret 또는 OPENVIDU_SECRET 환경변수");
                throw new IllegalStateException("OpenVidu 시크릿이 설정되지 않았습니다.");
            }
            
            String openviduUrl = "https://" + openviduDomain.trim();
            log.info("[OPENVIDU-INIT] ✅ OpenVidu URL 구성 완료:");
            log.info("[OPENVIDU-INIT]   - Final URL: {}", openviduUrl);
            log.info("[OPENVIDU-INIT]   - URL Length: {}", openviduUrl.length());
            
            // URL 파싱 및 검증
            String hostname = networkDiagnostics.extractHostFromUrl(openviduUrl);
            int port = networkDiagnostics.extractPortFromUrl(openviduUrl);
            log.info("[OPENVIDU-INIT] URL 파싱 결과:");
            log.info("[OPENVIDU-INIT]   - Extracted Hostname: {}", hostname);
            log.info("[OPENVIDU-INIT]   - Extracted Port: {}", port);
            
            if (hostname == null) {
                log.info("[OPENVIDU-INIT] ❌ URL 파싱 실패 - 잘못된 형식의 URL");
                throw new IllegalStateException("잘못된 형식의 OpenVidu URL: " + openviduUrl);
            }
            
            // DNS 조회 테스트
            log.info("[OPENVIDU-INIT] ========== DNS 조회 테스트 ==========");
            networkDiagnostics.performDnsLookup(hostname);
            
            // TCP 연결 테스트  
            log.info("[OPENVIDU-INIT] ========== TCP 연결 테스트 ==========");
            networkDiagnostics.performTcpConnectionTest(hostname, port, 5000);
            
            // OpenVidu 객체 생성
            log.info("[OPENVIDU-INIT] ========== OpenVidu 클라이언트 객체 생성 ==========");
            long openviduCreateStartTime = System.currentTimeMillis();
            log.info("[OPENVIDU-INIT] OpenVidu 객체 생성 시작:");
            log.info("[OPENVIDU-INIT]   - URL: {}", openviduUrl);
            log.info("[OPENVIDU-INIT]   - Secret Length: {}", openviduSecret.length());
            
            try {
                this.openVidu = new OpenVidu(openviduUrl, openviduSecret);
                long openviduCreateTime = System.currentTimeMillis() - openviduCreateStartTime;
                log.info("[OPENVIDU-INIT] ✅ OpenVidu 객체 생성 성공:");
                log.info("[OPENVIDU-INIT]   - 생성시간: {}ms", openviduCreateTime);
                log.info("[OPENVIDU-INIT]   - 객체 타입: {}", this.openVidu.getClass().getSimpleName());
                
            } catch (Exception openViduError) {
                long openviduCreateTime = System.currentTimeMillis() - openviduCreateStartTime;
                log.info("[OPENVIDU-INIT] ❌ OpenVidu 객체 생성 실패:");
                log.info("[OPENVIDU-INIT]   - URL: {}", openviduUrl);
                log.info("[OPENVIDU-INIT]   - 시도시간: {}ms", openviduCreateTime);
                log.info("[OPENVIDU-INIT]   - Error Type: {}", openViduError.getClass().getSimpleName());
                log.info("[OPENVIDU-INIT]   - Error Message: {}", openViduError.getMessage());
                
                if (openViduError.getCause() != null) {
                    log.info("[OPENVIDU-INIT]   - Root Cause Type: {}", 
                            openViduError.getCause().getClass().getSimpleName());
                    log.info("[OPENVIDU-INIT]   - Root Cause Message: {}", 
                            openViduError.getCause().getMessage());
                }
                
                // DNS 재확인
                log.info("[OPENVIDU-INIT] 객체 생성 실패 후 DNS 재확인:");
                networkDiagnostics.performDnsLookup(hostname);
                
                throw openViduError;
            }
            
            // 연결 테스트 (선택적)
            log.info("[OPENVIDU-INIT] ========== OpenVidu 서버 연결 상태 테스트 ==========");
            long connectionTestStartTime = System.currentTimeMillis();
            log.info("[OPENVIDU-INIT] getActiveSessions() 호출로 서버 연결 테스트 시작");
            
            try {
                List<Session> activeSessions = this.openVidu.getActiveSessions();
                long connectionTestTime = System.currentTimeMillis() - connectionTestStartTime;
                log.info("[OPENVIDU-INIT] ✅ OpenVidu 서버 연결 상태 테스트 성공:");
                log.info("[OPENVIDU-INIT]   - 테스트시간: {}ms", connectionTestTime);
                log.info("[OPENVIDU-INIT]   - 활성 세션 수: {}", activeSessions.size());
                log.info("[OPENVIDU-INIT]   - 서버 응답 정상");
                
                if (!activeSessions.isEmpty()) {
                    log.info("[OPENVIDU-INIT] 기존 활성 세션 목록:");
                    for (int i = 0; i < activeSessions.size(); i++) {
                        Session session = activeSessions.get(i);
                        log.info("[OPENVIDU-INIT]   Session[{}]: ID={}, Connections={}",
                                i + 1, session.getSessionId(), session.getActiveConnections().size());
                    }
                }
                
            } catch (Exception connectionTestError) {
                long connectionTestTime = System.currentTimeMillis() - connectionTestStartTime;
                log.info("[OPENVIDU-INIT] ⚠️ OpenVidu 서버 연결 상태 테스트 실패 (객체는 생성됨):");
                log.info("[OPENVIDU-INIT]   - 테스트시간: {}ms", connectionTestTime);
                log.info("[OPENVIDU-INIT]   - Error Type: {}", connectionTestError.getClass().getSimpleName());
                log.info("[OPENVIDU-INIT]   - Error Message: {}", connectionTestError.getMessage());
                
                if (connectionTestError.getCause() != null) {
                    log.info("[OPENVIDU-INIT]   - Root Cause: {}", 
                            connectionTestError.getCause().getClass().getSimpleName());
                    log.info("[OPENVIDU-INIT]   - Root Cause Message: {}", 
                            connectionTestError.getCause().getMessage());
                }
                
                // 연결 테스트 실패 시 추가 진단
                log.info("[OPENVIDU-INIT] 연결 실패 후 추가 네트워크 진단:");
                networkDiagnostics.performDnsLookup(hostname);
                networkDiagnostics.performTcpConnectionTest(hostname, port, 3000);
                
                // 연결 테스트 실패는 경고만 하고 초기화는 계속 진행
                log.info("[OPENVIDU-INIT] 연결 테스트는 실패했지만 초기화는 계속 진행합니다");
            }
            
            long totalInitTime = System.currentTimeMillis() - initStartTime;
            log.info("[OPENVIDU-INIT] ========== OpenVidu 초기화 완료 (운영) ==========");
            log.info("[OPENVIDU-INIT] 초기화 요약:");
            log.info("[OPENVIDU-INIT]   - 서버: {}", openviduUrl);
            log.info("[OPENVIDU-INIT]   - 전체 초기화 시간: {}ms", totalInitTime);
            log.info("[OPENVIDU-INIT]   - 상태: 정상 완료");
            log.info("[OPENVIDU-INIT] =================================================");
            
        } catch (Exception e) {
            long totalInitTime = System.currentTimeMillis() - initStartTime;
            log.info("[OPENVIDU-INIT] ========== OpenVidu 초기화 실패 (운영) ==========");
            log.info("[OPENVIDU-INIT] 실패 상세 정보:");
            log.info("[OPENVIDU-INIT]   - Domain: {}", openviduDomain);
            log.info("[OPENVIDU-INIT]   - 전체 시도 시간: {}ms", totalInitTime);
            log.info("[OPENVIDU-INIT]   - Error Type: {}", e.getClass().getSimpleName());
            log.info("[OPENVIDU-INIT]   - Error Message: {}", e.getMessage());
            
            if (e.getCause() != null) {
                log.info("[OPENVIDU-INIT]   - Root Cause Type: {}", e.getCause().getClass().getSimpleName());
                log.info("[OPENVIDU-INIT]   - Root Cause Message: {}", e.getCause().getMessage());
            }
            
            // 전체 네트워크 진단 수행
            log.info("[OPENVIDU-INIT] 실패 원인 파악을 위한 전체 네트워크 진단 수행:");
            try {
                String openviduUrl = "https://" + (openviduDomain != null ? openviduDomain.trim() : "unknown");
                networkDiagnostics.performFullDiagnostics(openviduUrl);
            } catch (Exception diagError) {
                log.info("[OPENVIDU-INIT] 네트워크 진단 중 오류: {}", diagError.getMessage());
            }
            
            log.info("[OPENVIDU-INIT] =================================================");
            log.info("[OPENVIDU-INIT] 초기화 실패는 애플리케이션 시작을 막지 않고 경고만 출력");
            log.info("[OPENVIDU-INIT] 런타임에 다시 시도할 수 있도록 openVidu는 null로 유지");
            log.info("[OPENVIDU-INIT] =================================================");
            
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
        log.info("[SESSION-CREATE] ========== 세션 생성 시작 ==========");
        log.info("[SESSION-CREATE] 요청 정보:");
        log.info("[SESSION-CREATE]   - Session Name: '{}'", sessionName);
        log.info("[SESSION-CREATE]   - Session Name Length: {}", sessionName != null ? sessionName.length() : 0);
        
        String sessionId = "session_" + sessionName;
        log.info("[SESSION-CREATE]   - Generated Session ID: '{}'", sessionId);
        log.info("[SESSION-CREATE]   - OpenVidu Server: https://{}", openviduDomain);
        
        // OpenVidu 객체 상태 확인
        if (this.openVidu == null) {
            log.info("[SESSION-CREATE] ❌ OpenVidu 객체가 null입니다 - 초기화 실패 상태");
            log.info("[SESSION-CREATE] 해결방법: 애플리케이션 재시작 또는 OpenVidu 서버 연결 확인");
            throw new SessionCreationException("OpenVidu 초기화 실패 - 세션 생성 불가: " + sessionName);
        }
        
        log.info("[SESSION-CREATE] OpenVidu 객체 상태: 정상");
        
        int attempt = 0;
        int maxAttempts = 3;
        Exception lastException = null;
        
        while (attempt < maxAttempts) {
            attempt++;
            log.info("[SESSION-CREATE] ========== 생성 시도 {}/{} ==========", attempt, maxAttempts);
            
            try {
                // DNS 재확인 (실패 시)
                if (attempt > 1) {
                    log.info("[SESSION-CREATE] 재시도 전 DNS 상태 확인:");
                    String hostname = networkDiagnostics.extractHostFromUrl("https://" + openviduDomain);
                    if (hostname != null) {
                        networkDiagnostics.performDnsLookup(hostname);
                    }
                }
                
                // 세션 프로퍼티 구성
                log.info("[SESSION-CREATE] SessionProperties 구성 시작:");
                SessionProperties sessionProperties = new SessionProperties.Builder()
                        .customSessionId(sessionId)
                        .mediaMode(MediaMode.ROUTED)   //ROUTED -> 서버 경유 연결(안정적), RELAYED -> P2P 연결(속도 지향)
                        .recordingMode(RecordingMode.MANUAL)    //MANUAL -> 필요할때만 녹화
                        .build();
                
                log.info("[SESSION-CREATE]   - Custom Session ID: {}", sessionProperties.customSessionId());
                log.info("[SESSION-CREATE]   - Media Mode: {}", sessionProperties.mediaMode());
                log.info("[SESSION-CREATE]   - Recording Mode: {}", sessionProperties.recordingMode());
                
                // OpenVidu 세션 생성
                log.info("[SESSION-CREATE] OpenVidu 서버에 세션 생성 요청 시작");
                long openviduStartTime = System.currentTimeMillis();
                
                Session openviduSession = openVidu.createSession(sessionProperties);
                
                long openviduElapsedTime = System.currentTimeMillis() - openviduStartTime;
                log.info("[SESSION-CREATE] ✅ OpenVidu 세션 생성 성공:");
                log.info("[SESSION-CREATE]   - 서버 응답 시간: {}ms", openviduElapsedTime);
                log.info("[SESSION-CREATE]   - OpenVidu Session ID: {}", openviduSession.getSessionId());
                log.info("[SESSION-CREATE]   - Session Token Count: {}", openviduSession.getActiveConnections().size());
                log.info("[SESSION-CREATE]   - Created At: {}", openviduSession.createdAt());
                
                // DB에 VideoSession 엔티티 생성 및 저장
                log.info("[SESSION-CREATE] DB에 VideoSession 엔티티 저장 시작");
                VideoSession videoSession = VideoSession.builder()
                        .sessionId(sessionId)
                        .sessionName(sessionName)
                        .build();
                
                log.info("[SESSION-CREATE] VideoSession 엔티티 구성 완료:");
                log.info("[SESSION-CREATE]   - Entity Session ID: {}", videoSession.getSessionId());
                log.info("[SESSION-CREATE]   - Entity Session Name: {}", videoSession.getSessionName());
                
                long dbStartTime = System.currentTimeMillis();
                VideoSession savedSession = videoSessionRepository.save(videoSession);
                long dbElapsedTime = System.currentTimeMillis() - dbStartTime;
                
                log.info("[SESSION-CREATE] ✅ DB 저장 성공:");
                log.info("[SESSION-CREATE]   - DB 저장 시간: {}ms", dbElapsedTime);
                log.info("[SESSION-CREATE]   - Entity ID: {}", savedSession.getId());
                log.info("[SESSION-CREATE]   - Entity Session ID: {}", savedSession.getSessionId());
                log.info("[SESSION-CREATE]   - Entity Session Name: {}", savedSession.getSessionName());
                
                log.info("[SESSION-CREATE] ========== 세션 생성 완료 ==========");
                log.info("[SESSION-CREATE] 성공 요약:");
                log.info("[SESSION-CREATE]   - Session Name: {}", sessionName);
                log.info("[SESSION-CREATE]   - Session ID: {}", sessionId);
                log.info("[SESSION-CREATE]   - 시도 횟수: {}/{}", attempt, maxAttempts);
                log.info("[SESSION-CREATE]   - OpenVidu 응답시간: {}ms", openviduElapsedTime);
                log.info("[SESSION-CREATE]   - DB 저장시간: {}ms", dbElapsedTime);
                log.info("[SESSION-CREATE] ===========================================");
                
                return savedSession;
                
            } catch (OpenViduJavaClientException e) {
                lastException = e;
                long attemptTime = System.currentTimeMillis();
                
                log.info("[SESSION-CREATE] ❌ OpenVidu 클라이언트 예외 발생 (시도 {}/{}):", attempt, maxAttempts);
                log.info("[SESSION-CREATE]   - Exception Type: {}", e.getClass().getSimpleName());
                log.info("[SESSION-CREATE]   - HTTP Status: OpenViduJavaClientException");
                log.info("[SESSION-CREATE]   - Error Message: {}", e.getMessage());
                
                if (e.getCause() != null) {
                    log.info("[SESSION-CREATE]   - Root Cause Type: {}", e.getCause().getClass().getSimpleName());
                    log.info("[SESSION-CREATE]   - Root Cause Message: {}", e.getCause().getMessage());
                    
                    // DNS 관련 오류 특별 처리
                    if (e.getCause().getMessage() != null && 
                        e.getCause().getMessage().contains("Name or service not known")) {
                        log.info("[SESSION-CREATE]   - 🔍 DNS 조회 실패 감지!");
                        log.info("[SESSION-CREATE]   - 원인: OpenVidu 서버 도메인을 IP로 변환 실패");
                        log.info("[SESSION-CREATE]   - 확인사항: DNS 설정, /etc/hosts, 네트워크 연결");
                        
                        // 즉시 DNS 재진단
                        String hostname = networkDiagnostics.extractHostFromUrl("https://" + openviduDomain);
                        if (hostname != null) {
                            log.info("[SESSION-CREATE] DNS 실패 후 긴급 재진단:");
                            networkDiagnostics.performDnsLookup(hostname);
                        }
                    }
                }
                
                if (attempt < maxAttempts) {
                    long backoffTime = attempt * 1000L; // 1초, 2초, 3초 백오프
                    log.info("[SESSION-CREATE] {}ms 후 재시도 예정... (다음 시도: {}/{})", 
                            backoffTime, attempt + 1, maxAttempts);
                    
                    try {
                        Thread.sleep(backoffTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.info("[SESSION-CREATE] 재시도 대기 중 인터럽트 발생");
                        break;
                    }
                } else {
                    log.info("[SESSION-CREATE] 모든 재시도 시도 완료 - 최종 실패");
                }
                
            } catch (Exception e) {
                lastException = e;
                log.info("[SESSION-CREATE] ❌ 예상치 못한 예외 발생 (시도 {}/{}):", attempt, maxAttempts);
                log.info("[SESSION-CREATE]   - Exception Type: {}", e.getClass().getSimpleName());
                log.info("[SESSION-CREATE]   - Error Message: {}", e.getMessage());
                
                if (e.getCause() != null) {
                    log.info("[SESSION-CREATE]   - Root Cause Type: {}", e.getCause().getClass().getSimpleName());
                    log.info("[SESSION-CREATE]   - Root Cause Message: {}", e.getCause().getMessage());
                }
                
                if (attempt < maxAttempts) {
                    long backoffTime = attempt * 1000L;
                    log.info("[SESSION-CREATE] {}ms 후 재시도...", backoffTime);
                    
                    try {
                        Thread.sleep(backoffTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.info("[SESSION-CREATE] 재시도 대기 중 인터럽트 발생");
                        break;
                    }
                } else {
                    log.info("[SESSION-CREATE] 모든 재시도 시도 완료 - 최종 실패");
                }
            }
        }
        
        // 최종 실패 처리
        log.info("[SESSION-CREATE] ========== 세션 생성 최종 실패 ==========");
        log.info("[SESSION-CREATE] 실패 요약:");
        log.info("[SESSION-CREATE]   - Session Name: {}", sessionName);
        log.info("[SESSION-CREATE]   - 총 시도 횟수: {}", maxAttempts);
        log.info("[SESSION-CREATE]   - 마지막 오류: {}", 
                lastException != null ? lastException.getClass().getSimpleName() : "Unknown");
        log.info("[SESSION-CREATE]   - 마지막 오류 메시지: {}", 
                lastException != null ? lastException.getMessage() : "Unknown");
        
        // 최종 실패 시 전체 네트워크 진단
        log.info("[SESSION-CREATE] 최종 실패 - 전체 네트워크 진단 수행:");
        try {
            networkDiagnostics.performFullDiagnostics("https://" + openviduDomain);
        } catch (Exception diagError) {
            log.info("[SESSION-CREATE] 네트워크 진단 중 오류: {}", diagError.getMessage());
        }
        
        log.info("[SESSION-CREATE] =============================================");
        
        String errorMessage = String.format("세션 생성 최종 실패 - 이름: %s, 시도횟수: %d, 마지막오류: %s", 
                sessionName, maxAttempts, lastException != null ? lastException.getMessage() : "Unknown");
        
        throw new SessionCreationException(errorMessage);
    }

    /**
     * 세션에 참가하고, WebRTC용 토큰을 발급
     * @param sessionId 참가할 SessionId
     * @param userName  참가자 사용자명
     * @return WebRTC 연결을 위한 토큰
     */
    public String joinSession(String sessionId, String userName){
        log.info("[SESSION-JOIN] ========== 세션 참가 시작 ==========");
        log.info("[SESSION-JOIN] 요청 정보:");
        log.info("[SESSION-JOIN]   - Session ID: '{}'", sessionId);
        log.info("[SESSION-JOIN]   - User Name: '{}'", userName);
        log.info("[SESSION-JOIN]   - OpenVidu Server: https://{}", openviduDomain);
        
        // OpenVidu 객체 상태 확인
        if (this.openVidu == null) {
            log.info("[SESSION-JOIN] ❌ OpenVidu 객체가 null입니다 - 초기화 실패 상태");
            log.info("[SESSION-JOIN] 해결방법: 애플리케이션 재시작 또는 OpenVidu 서버 연결 확인");
            throw new SessionCreationException("OpenVidu 초기화 실패 - 세션 참가 불가");
        }
        
        try{
            // 1. 회원 존재 여부 확인
            log.info("[SESSION-JOIN] ========== 회원 정보 확인 ==========");
            log.info("[SESSION-JOIN] DB에서 회원 정보 조회 시작: '{}'", userName);
            
            long memberStartTime = System.currentTimeMillis();
            Optional<Member> byNicknameAndIsDeletedFalse = memberRepository.findByNicknameAndIsDeletedFalse(userName);
            long memberElapsedTime = System.currentTimeMillis() - memberStartTime;
            
            if(byNicknameAndIsDeletedFalse.isEmpty()){
                log.info("[SESSION-JOIN] ❌ 회원 정보 조회 실패:");
                log.info("[SESSION-JOIN]   - 조회 시간: {}ms", memberElapsedTime);
                log.info("[SESSION-JOIN]   - 조회 결과: 회원 없음");
                log.info("[SESSION-JOIN]   - 확인사항: 회원가입 여부, 계정 삭제 여부");
                throw new RuntimeException("회원 정보를 찾을 수 없습니다: " + userName);
            }
            
            Member member = byNicknameAndIsDeletedFalse.get();
            log.info("[SESSION-JOIN] ✅ 회원 정보 조회 성공:");
            log.info("[SESSION-JOIN]   - 조회 시간: {}ms", memberElapsedTime);
            log.info("[SESSION-JOIN]   - 회원 ID: {}", member.getId());
            log.info("[SESSION-JOIN]   - 회원 닉네임: {}", member.getNickname());
            log.info("[SESSION-JOIN]   - 삭제 여부: {}", member.getIsDeleted());

            // 2. VideoSession 존재 여부 확인
            log.info("[SESSION-JOIN] ========== 비디오 세션 확인 ==========");
            log.info("[SESSION-JOIN] DB에서 VideoSession 조회 시작: '{}'", sessionId);
            
            long sessionStartTime = System.currentTimeMillis();
            VideoSession videoSession = videoSessionRepository
                    .findBySessionId(sessionId)
                    .orElseThrow(() -> {
                        long sessionElapsedTime = System.currentTimeMillis() - sessionStartTime;
                        log.info("[SESSION-JOIN] ❌ VideoSession 조회 실패:");
                        log.info("[SESSION-JOIN]   - 조회 시간: {}ms", sessionElapsedTime);
                        log.info("[SESSION-JOIN]   - Session ID: '{}'", sessionId);
                        log.info("[SESSION-JOIN]   - 확인사항: 세션 생성 여부, Session ID 정확성");
                        return new SessionNotFoundException("세션 ID: " + sessionId + "가 DB에 존재하지 않습니다");
                    });
            
            long sessionElapsedTime = System.currentTimeMillis() - sessionStartTime;
            log.info("[SESSION-JOIN] ✅ VideoSession 조회 성공:");
            log.info("[SESSION-JOIN]   - 조회 시간: {}ms", sessionElapsedTime);
            log.info("[SESSION-JOIN]   - Entity ID: {}", videoSession.getId());
            log.info("[SESSION-JOIN]   - Session ID: {}", videoSession.getSessionId());
            log.info("[SESSION-JOIN]   - Session Name: {}", videoSession.getSessionName());

            // 3. OpenVidu 세션 상태 확인
            log.info("[SESSION-JOIN] ========== OpenVidu 세션 상태 확인 ==========");
            log.info("[SESSION-JOIN] OpenVidu 서버에서 활성 세션 조회 시작");
            
            long openviduStartTime = System.currentTimeMillis();
            Session openviduSession = openVidu.getActiveSession(sessionId);
            long openviduElapsedTime = System.currentTimeMillis() - openviduStartTime;
            
            if(openviduSession == null){
                log.info("[SESSION-JOIN] ❌ OpenVidu 세션 조회 실패:");
                log.info("[SESSION-JOIN]   - 조회 시간: {}ms", openviduElapsedTime);
                log.info("[SESSION-JOIN]   - Session ID: '{}'", sessionId);
                log.info("[SESSION-JOIN]   - 원인: 세션이 OpenVidu 서버에 존재하지 않음");
                log.info("[SESSION-JOIN]   - 확인사항: 세션 생성 여부, 세션 만료 여부");
                
                // DNS 재확인
                log.info("[SESSION-JOIN] OpenVidu 세션 조회 실패 후 DNS 상태 확인:");
                String hostname = networkDiagnostics.extractHostFromUrl("https://" + openviduDomain);
                if (hostname != null) {
                    networkDiagnostics.performDnsLookup(hostname);
                }
                
                throw new SessionNotFoundException("OpenVidu 서버에 세션 ID: " + sessionId + "가 없습니다");
            }
            
            log.info("[SESSION-JOIN] ✅ OpenVidu 세션 조회 성공:");
            log.info("[SESSION-JOIN]   - 조회 시간: {}ms", openviduElapsedTime);
            log.info("[SESSION-JOIN]   - OpenVidu Session ID: {}", openviduSession.getSessionId());
            log.info("[SESSION-JOIN]   - 활성 연결 수: {}", openviduSession.getActiveConnections().size());
            log.info("[SESSION-JOIN]   - 세션 생성 시간: {}", openviduSession.createdAt());
            
            // 기존 연결 정보 출력
            if (!openviduSession.getActiveConnections().isEmpty()) {
                log.info("[SESSION-JOIN] 기존 활성 연결 목록:");
                for (int i = 0; i < openviduSession.getActiveConnections().size(); i++) {
                    Connection conn = openviduSession.getActiveConnections().get(i);
                    log.info("[SESSION-JOIN]   Connection[{}]: ID={}, Type={}, Role={}", 
                            i + 1, conn.getConnectionId(), conn.getType(), conn.getRole());
                }
            }

            // 4. 연결 속성 구성
            log.info("[SESSION-JOIN] ========== 연결 속성 구성 ==========");
            String connectionData = "{\"username\":\"" + userName + "\"}";
            log.info("[SESSION-JOIN] ConnectionProperties 구성:");
            log.info("[SESSION-JOIN]   - Type: WEBRTC (저지연 WebRTC 사용)");
            log.info("[SESSION-JOIN]   - Data: {}", connectionData);
            log.info("[SESSION-JOIN]   - Role: PUBLISHER (송수신 동시에)");
            
            ConnectionProperties connectionProperties = new ConnectionProperties.Builder()
                    .type(ConnectionType.WEBRTC)    //저지연 WebRTC 사용
                    .data(connectionData)    //연결에 대한 추가정보, json 형태로 전달
                    .role(OpenViduRole.PUBLISHER)   //Publisher : 송수신 동시에, SUBSCRIBER 수신만, MODERATOR 관리자
                    .build();

            // 5. 토큰 생성
            log.info("[SESSION-JOIN] ========== 토큰 생성 ==========");
            log.info("[SESSION-JOIN] OpenVidu 서버에 Connection 생성 요청 시작");
            
            long tokenStartTime = System.currentTimeMillis();
            Connection connection = openviduSession.createConnection(connectionProperties);
            long tokenElapsedTime = System.currentTimeMillis() - tokenStartTime;
            
            String originalToken = connection.getToken();
            log.info("[SESSION-JOIN] ✅ Connection 생성 성공:");
            log.info("[SESSION-JOIN]   - 생성 시간: {}ms", tokenElapsedTime);
            log.info("[SESSION-JOIN]   - Connection ID: {}", connection.getConnectionId());
            log.info("[SESSION-JOIN]   - Original Token Length: {}", originalToken.length());
            log.info("[SESSION-JOIN]   - Token Type: {}", originalToken.startsWith("tok_") ? "WebSocket Link" : "Direct Token");

            // 6. 토큰 포맷 처리
            String token = originalToken;
            log.info("[SESSION-JOIN] 토큰 포맷 처리:");
            
            if (originalToken.startsWith("tok_")) { //웹소켓 링크 형식 token이 전달되지 않았을 때 처리
                token = String.format("wss://" + openviduDomain + "?sessionId=%s&token=%s",
                        sessionId, originalToken);
                log.info("[SESSION-JOIN]   - 포맷 변환: WebSocket URL 형식으로 변환");
                log.info("[SESSION-JOIN]   - Final Token Length: {}", token.length());
            } else {
                log.info("[SESSION-JOIN]   - 포맷 유지: 직접 토큰 형식 사용");
            }
            
            log.debug("[OPENVIDU] session connect token = {}", originalToken);
            log.debug("[OPENVIDU] connection success / ID = {}", connection.getConnectionId());

            // 7. 참가자 정보 저장
            log.info("[SESSION-JOIN] ========== 참가자 정보 저장 ==========");
            log.info("[SESSION-JOIN] Participant 엔티티 생성 및 저장 시작");
            
            Participant participant = Participant.builder()
                    .connectionId(connection.getConnectionId())
                    .token(token)
                    .username(userName)
                    .videoSession(videoSession)
                    .member(member)
                    .build();
            
            log.info("[SESSION-JOIN] Participant 엔티티 구성 완료:");
            log.info("[SESSION-JOIN]   - Connection ID: {}", participant.getConnectionId());
            log.info("[SESSION-JOIN]   - Username: {}", participant.getUsername());
            log.info("[SESSION-JOIN]   - Token Length: {}", participant.getToken().length());
            log.info("[SESSION-JOIN]   - Member ID: {}", participant.getMember().getId());
            log.info("[SESSION-JOIN]   - VideoSession ID: {}", participant.getVideoSession().getId());
            
            long participantStartTime = System.currentTimeMillis();
            Participant savedParticipant = participantRepository.save(participant);
            long participantElapsedTime = System.currentTimeMillis() - participantStartTime;
            
            log.info("[SESSION-JOIN] ✅ Participant 저장 성공:");
            log.info("[SESSION-JOIN]   - 저장 시간: {}ms", participantElapsedTime);
            log.info("[SESSION-JOIN]   - Entity ID: {}", savedParticipant.getId());

            // 8. 최종 성공 요약
            log.info("[SESSION-JOIN] ========== 세션 참가 완료 ==========");
            log.info("[SESSION-JOIN] 성공 요약:");
            log.info("[SESSION-JOIN]   - Session ID: {}", sessionId);
            log.info("[SESSION-JOIN]   - User Name: {}", userName);
            log.info("[SESSION-JOIN]   - Connection ID: {}", connection.getConnectionId());
            log.info("[SESSION-JOIN]   - 회원 조회 시간: {}ms", memberElapsedTime);
            log.info("[SESSION-JOIN]   - 세션 조회 시간: {}ms", sessionElapsedTime);
            log.info("[SESSION-JOIN]   - OpenVidu 조회 시간: {}ms", openviduElapsedTime);
            log.info("[SESSION-JOIN]   - 토큰 생성 시간: {}ms", tokenElapsedTime);
            log.info("[SESSION-JOIN]   - 참가자 저장 시간: {}ms", participantElapsedTime);
            log.info("[SESSION-JOIN] ==========================================");

            return token;
            
        }catch (OpenViduJavaClientException e) {
            log.info("[SESSION-JOIN] ❌ OpenVidu 클라이언트 예외 발생:");
            log.info("[SESSION-JOIN]   - Exception Type: {}", e.getClass().getSimpleName());
            log.info("[SESSION-JOIN]   - HTTP Status: OpenViduJavaClientException");
            log.info("[SESSION-JOIN]   - Error Message: {}", e.getMessage());
            
            if (e.getCause() != null) {
                log.info("[SESSION-JOIN]   - Root Cause Type: {}", e.getCause().getClass().getSimpleName());
                log.info("[SESSION-JOIN]   - Root Cause Message: {}", e.getCause().getMessage());
                
                // DNS 관련 오류 특별 처리
                if (e.getCause().getMessage() != null && 
                    e.getCause().getMessage().contains("Name or service not known")) {
                    log.info("[SESSION-JOIN]   - 🔍 DNS 조회 실패 감지!");
                    log.info("[SESSION-JOIN]   - 원인: OpenVidu 서버 도메인을 IP로 변환 실패");
                    log.info("[SESSION-JOIN]   - 확인사항: DNS 설정, /etc/hosts, 네트워크 연결");
                    
                    // 즉시 DNS 재진단
                    String hostname = networkDiagnostics.extractHostFromUrl("https://" + openviduDomain);
                    if (hostname != null) {
                        log.info("[SESSION-JOIN] DNS 실패 후 긴급 재진단:");
                        networkDiagnostics.performDnsLookup(hostname);
                    }
                }
            }
            
            // 실패 시 전체 네트워크 진단
            log.info("[SESSION-JOIN] OpenVidu 예외 발생 - 네트워크 진단 수행:");
            try {
                networkDiagnostics.performFullDiagnostics("https://" + openviduDomain);
            } catch (Exception diagError) {
                log.info("[SESSION-JOIN] 네트워크 진단 중 오류: {}", diagError.getMessage());
            }
            
            throw new SessionCreationException("토큰 발급 실패 (OpenVidu 오류) - 세션: " + sessionId + 
                    ", 사용자: " + userName + ", 원인: " + e.getMessage());
            
        }catch (Exception e){
            log.info("[SESSION-JOIN] ❌ 예상치 못한 예외 발생:");
            log.info("[SESSION-JOIN]   - Exception Type: {}", e.getClass().getSimpleName());
            log.info("[SESSION-JOIN]   - Error Message: {}", e.getMessage());
            
            if (e.getCause() != null) {
                log.info("[SESSION-JOIN]   - Root Cause Type: {}", e.getCause().getClass().getSimpleName());
                log.info("[SESSION-JOIN]   - Root Cause Message: {}", e.getCause().getMessage());
            }
            
            log.info("[SESSION-JOIN] ========== 세션 참가 최종 실패 ==========");
            log.info("[SESSION-JOIN] 실패 요약:");
            log.info("[SESSION-JOIN]   - Session ID: {}", sessionId);
            log.info("[SESSION-JOIN]   - User Name: {}", userName);
            log.info("[SESSION-JOIN]   - Error: {}", e.getClass().getSimpleName());
            log.info("[SESSION-JOIN] =============================================");
            
            throw new SessionCreationException("토큰 발급 실패 - 세션: " + sessionId + 
                    ", 사용자: " + userName + ", 원인: " + e.getMessage());
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
                    .orElseThrow(() -> new SessionNotFoundException("활성 세션 ID: " + sessionId + "를 찾을 수 없습니다"));

            Session openviduSession = openVidu.getActiveSession(sessionId);
            if(openviduSession == null){
                throw new SessionNotFoundException("OpenVidu 서버에 녹화용 세션 ID: " + sessionId + "가 없습니다");
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
                throw new RecordingAlreadyStartedException("세션 " + sessionId + "에서 이미 녹화가 진행 중입니다");
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
                throw new RecordingFailedException("세션 " + sessionId + " 녹화 시작 실패: " + e.getMessage());
            }
        }catch (Exception e){
            log.error("[OPENVIDU] session {} / recording failed in outside {}",sessionId,  e.getMessage());
            throw new RecordingFailedException("세션 " + sessionId + " 녹화 전체 실패: " + e.getMessage());
        }
    }

    public void endSession(String sessionId) {
    try {
        VideoSession videoSession = videoSessionRepository
                .findBySessionId(sessionId)
                .orElseThrow(SessionNotFoundException::new);

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
            throw new RecordingFailedException("세션 " + sessionId + " 녹화 중단 실패: " + e.getMessage());
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
            throw new RecordingFailedException("세션 " + sessionId + " 녹화 목록 조회 실패: " + e.getMessage());
        }
    }

    public Recording getRecording(String recordingId) {
        try {
            return openVidu.getRecording(recordingId);
        } catch (Exception e) {
            log.error("녹화 정보 조회 실패: recordingId={}, error={}", recordingId, e.getMessage());
            throw new RecordingFailedException("녹화 ID " + recordingId + " 정보 조회 실패: " + e.getMessage());
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
                    .orElseThrow(() -> new SessionNotFoundException("화면 공유용 활성 세션 ID: " + sessionId + "를 찾을 수 없습니다"));

            Session openviduSession = openVidu.getActiveSession(sessionId);
            if (openviduSession == null) {
                throw new SessionNotFoundException("화면 공유용 OpenVidu 세션 ID: " + sessionId + "가 없습니다");
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
