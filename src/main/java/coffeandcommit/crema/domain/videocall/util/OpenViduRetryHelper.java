package coffeandcommit.crema.domain.videocall.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Slf4j
@Component
public class OpenViduRetryHelper {
    
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long DEFAULT_BASE_BACKOFF_MS = 1000L;
    private static final double DEFAULT_BACKOFF_MULTIPLIER = 1.5;
    
    private final NetworkDiagnostics networkDiagnostics;
    
    public OpenViduRetryHelper(NetworkDiagnostics networkDiagnostics) {
        this.networkDiagnostics = networkDiagnostics;
    }
    
    /**
     * 기본 설정으로 재시도 실행
     */
    public <T> T executeWithRetry(String operation, Supplier<T> action) {
        return executeWithRetry(operation, action, DEFAULT_MAX_ATTEMPTS, DEFAULT_BASE_BACKOFF_MS);
    }
    
    /**
     * 커스텀 설정으로 재시도 실행
     */
    public <T> T executeWithRetry(String operation, Supplier<T> action, int maxAttempts, long baseBackoffMs) {
        log.info("[RETRY] ========== {} 작업 시작 ==========", operation);
        log.info("[RETRY] 재시도 설정:");
        log.info("[RETRY]   - 최대 시도 횟수: {}", maxAttempts);
        log.info("[RETRY]   - 기본 백오프: {}ms", baseBackoffMs);
        log.info("[RETRY]   - 백오프 배수: {}", DEFAULT_BACKOFF_MULTIPLIER);
        
        Exception lastException = null;
        long totalStartTime = System.currentTimeMillis();
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            log.info("[RETRY] ========== 시도 {}/{} ==========", attempt, maxAttempts);
            log.info("[RETRY] 작업: '{}'", operation);
            
            long attemptStartTime = System.currentTimeMillis();
            
            try {
                // 첫 시도가 아닌 경우 네트워크 상태 재확인
                if (attempt > 1) {
                    log.info("[RETRY] 재시도 전 상태 확인:");
                    // 간단한 네트워크 체크 (필요시 구현)
                    log.info("[RETRY]   - 이전 시도 실패 후 재시도 진행");
                }
                
                T result = action.get();
                long attemptElapsedTime = System.currentTimeMillis() - attemptStartTime;
                long totalElapsedTime = System.currentTimeMillis() - totalStartTime;
                
                log.info("[RETRY] ✅ 작업 성공:");
                log.info("[RETRY]   - 작업: '{}'", operation);
                log.info("[RETRY]   - 성공 시도: {}/{}", attempt, maxAttempts);
                log.info("[RETRY]   - 이번 시도 시간: {}ms", attemptElapsedTime);
                log.info("[RETRY]   - 총 소요 시간: {}ms", totalElapsedTime);
                log.info("[RETRY] ==========================================");
                
                return result;
                
            } catch (Exception e) {
                lastException = e;
                long attemptElapsedTime = System.currentTimeMillis() - attemptStartTime;
                
                log.info("[RETRY] ❌ 작업 실패 (시도 {}/{}):", attempt, maxAttempts);
                log.info("[RETRY]   - 작업: '{}'", operation);
                log.info("[RETRY]   - 시도 시간: {}ms", attemptElapsedTime);
                log.info("[RETRY]   - Error Type: {}", e.getClass().getSimpleName());
                log.info("[RETRY]   - Error Message: {}", e.getMessage());
                
                if (e.getCause() != null) {
                    log.info("[RETRY]   - Root Cause Type: {}", e.getCause().getClass().getSimpleName());
                    log.info("[RETRY]   - Root Cause Message: {}", e.getCause().getMessage());
                }
                
                // DNS 관련 오류 특별 처리
                if (isDnsRelatedError(e)) {
                    log.info("[RETRY]   - 🔍 DNS 관련 오류 감지!");
                    log.info("[RETRY]   - 진단: 도메인 이름 조회 실패");
                    log.info("[RETRY]   - 권장사항: DNS 설정, 네트워크 연결 확인");
                }
                
                // 연결 관련 오류 감지
                if (isConnectionRelatedError(e)) {
                    log.info("[RETRY]   - 🔍 연결 관련 오류 감지!");
                    log.info("[RETRY]   - 진단: 서버 연결 실패");
                    log.info("[RETRY]   - 권장사항: 서버 상태, 방화벽, 포트 확인");
                }
                
                if (attempt < maxAttempts) {
                    long backoffTime = calculateBackoff(attempt, baseBackoffMs);
                    log.info("[RETRY] 재시도 예정:");
                    log.info("[RETRY]   - 백오프 시간: {}ms", backoffTime);
                    log.info("[RETRY]   - 다음 시도: {}/{}", attempt + 1, maxAttempts);
                    log.info("[RETRY]   - 예상 재시도 시각: {}ms 후", backoffTime);
                    
                    try {
                        Thread.sleep(backoffTime);
                        log.info("[RETRY] 백오프 완료 - 재시도 진행");
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.info("[RETRY] ⚠️ 재시도 대기 중 인터럽트 발생 - 작업 중단");
                        long totalElapsedTime = System.currentTimeMillis() - totalStartTime;
                        log.info("[RETRY] 인터럽트로 인한 조기 종료:");
                        log.info("[RETRY]   - 작업: '{}'", operation);
                        log.info("[RETRY]   - 완료된 시도: {}/{}", attempt, maxAttempts);
                        log.info("[RETRY]   - 총 소요 시간: {}ms", totalElapsedTime);
                        throw new RuntimeException("재시도 중 인터럽트 발생: " + operation, ie);
                    }
                } else {
                    log.info("[RETRY] 모든 재시도 시도 완료 - 최종 실패");
                }
            }
        }
        
        // 최종 실패 처리
        long totalElapsedTime = System.currentTimeMillis() - totalStartTime;
        log.info("[RETRY] ========== {} 작업 최종 실패 ==========", operation);
        log.info("[RETRY] 실패 요약:");
        log.info("[RETRY]   - 작업: '{}'", operation);
        log.info("[RETRY]   - 총 시도 횟수: {}", maxAttempts);
        log.info("[RETRY]   - 총 소요 시간: {}ms", totalElapsedTime);
        log.info("[RETRY]   - 평균 시도 시간: {}ms", totalElapsedTime / maxAttempts);
        log.info("[RETRY]   - 마지막 오류: {}", 
                lastException != null ? lastException.getClass().getSimpleName() : "Unknown");
        log.info("[RETRY]   - 마지막 오류 메시지: {}", 
                lastException != null ? lastException.getMessage() : "Unknown");
        
        // 최종 실패 시 진단 정보 제공
        if (lastException != null) {
            log.info("[RETRY] 실패 원인 분석:");
            
            if (isDnsRelatedError(lastException)) {
                log.info("[RETRY]   - 주요 원인: DNS 조회 실패");
                log.info("[RETRY]   - 해결방법: ");
                log.info("[RETRY]     1. DNS 서버 설정 확인 (/etc/resolv.conf)");
                log.info("[RETRY]     2. /etc/hosts 파일 확인");
                log.info("[RETRY]     3. 네트워크 연결 상태 확인");
                log.info("[RETRY]     4. 도메인 이름 정확성 확인");
            } else if (isConnectionRelatedError(lastException)) {
                log.info("[RETRY]   - 주요 원인: 서버 연결 실패");
                log.info("[RETRY]   - 해결방법: ");
                log.info("[RETRY]     1. OpenVidu 서버 상태 확인");
                log.info("[RETRY]     2. 방화벽 설정 확인");
                log.info("[RETRY]     3. 포트 접근 가능성 확인");
                log.info("[RETRY]     4. 네트워크 라우팅 확인");
            } else {
                log.info("[RETRY]   - 주요 원인: 기타 오류");
                log.info("[RETRY]   - 해결방법: 로그 및 에러 메시지 분석 필요");
            }
        }
        
        log.info("[RETRY] =============================================");
        
        // 최종 실패 시 예외 발생
        String errorMessage = String.format("작업 최종 실패 - 작업: %s, 시도횟수: %d, 총시간: %dms", 
                operation, maxAttempts, totalElapsedTime);
        
        if (lastException != null) {
            throw new RuntimeException(errorMessage, lastException);
        } else {
            throw new RuntimeException(errorMessage);
        }
    }
    
    /**
     * 재시도 없이 단일 실행 (로깅만)
     */
    public <T> T executeSingle(String operation, Supplier<T> action) {
        log.info("[SINGLE] ========== {} 작업 시작 ==========", operation);
        long startTime = System.currentTimeMillis();
        
        try {
            T result = action.get();
            long elapsedTime = System.currentTimeMillis() - startTime;
            
            log.info("[SINGLE] ✅ 작업 성공:");
            log.info("[SINGLE]   - 작업: '{}'", operation);
            log.info("[SINGLE]   - 소요 시간: {}ms", elapsedTime);
            log.info("[SINGLE] ==========================================");
            
            return result;
            
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            
            log.info("[SINGLE] ❌ 작업 실패:");
            log.info("[SINGLE]   - 작업: '{}'", operation);
            log.info("[SINGLE]   - 소요 시간: {}ms", elapsedTime);
            log.info("[SINGLE]   - Error Type: {}", e.getClass().getSimpleName());
            log.info("[SINGLE]   - Error Message: {}", e.getMessage());
            
            if (e.getCause() != null) {
                log.info("[SINGLE]   - Root Cause Type: {}", e.getCause().getClass().getSimpleName());
                log.info("[SINGLE]   - Root Cause Message: {}", e.getCause().getMessage());
            }
            
            log.info("[SINGLE] ==========================================");
            throw e;
        }
    }
    
    /**
     * 백오프 시간 계산 (지수 백오프)
     */
    private long calculateBackoff(int attempt, long baseBackoffMs) {
        // 지수 백오프: baseBackoff * (multiplier ^ (attempt - 1))
        double backoff = baseBackoffMs * Math.pow(DEFAULT_BACKOFF_MULTIPLIER, attempt - 1);
        
        // 최대 백오프 제한 (30초)
        long maxBackoff = 30000L;
        long calculatedBackoff = Math.min((long) backoff, maxBackoff);
        
        // 지터 추가 (±20% 랜덤)
        double jitterFactor = 0.8 + (Math.random() * 0.4); // 0.8 ~ 1.2
        long finalBackoff = (long) (calculatedBackoff * jitterFactor);
        
        log.info("[RETRY] 백오프 계산:");
        log.info("[RETRY]   - 시도 번호: {}", attempt);
        log.info("[RETRY]   - 기본 백오프: {}ms", baseBackoffMs);
        log.info("[RETRY]   - 계산된 백오프: {}ms", calculatedBackoff);
        log.info("[RETRY]   - 지터 적용 후: {}ms", finalBackoff);
        
        return Math.max(finalBackoff, 100L); // 최소 100ms
    }
    
    /**
     * DNS 관련 오류인지 확인
     */
    private boolean isDnsRelatedError(Exception e) {
        if (e == null) return false;
        
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("name or service not known") ||
                lowerMessage.contains("unknownhostexception") ||
                lowerMessage.contains("nodename nor servname provided") ||
                lowerMessage.contains("no address associated with hostname")) {
                return true;
            }
        }
        
        // Cause 체크
        Throwable cause = e.getCause();
        if (cause != null) {
            String causeMessage = cause.getMessage();
            if (causeMessage != null) {
                String lowerCauseMessage = causeMessage.toLowerCase();
                return lowerCauseMessage.contains("name or service not known") ||
                       lowerCauseMessage.contains("unknownhostexception") ||
                       cause.getClass().getSimpleName().contains("UnknownHost");
            }
        }
        
        return e.getClass().getSimpleName().contains("UnknownHost");
    }
    
    /**
     * 연결 관련 오류인지 확인
     */
    private boolean isConnectionRelatedError(Exception e) {
        if (e == null) return false;
        
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("connection refused") ||
                lowerMessage.contains("connection timeout") ||
                lowerMessage.contains("connection timed out") ||
                lowerMessage.contains("network unreachable") ||
                lowerMessage.contains("no route to host")) {
                return true;
            }
        }
        
        // Exception 타입 체크
        String exceptionName = e.getClass().getSimpleName().toLowerCase();
        return exceptionName.contains("connect") || 
               exceptionName.contains("timeout") ||
               exceptionName.contains("socket");
    }
    
    /**
     * 네트워크 진단과 함께 재시도 (고급 기능)
     */
    public <T> T executeWithNetworkDiagnostics(String operation, Supplier<T> action, String targetUrl) {
        log.info("[RETRY-DIAG] ========== {} 작업 시작 (네트워크 진단 포함) ==========", operation);
        
        try {
            return executeWithRetry(operation, action);
        } catch (Exception e) {
            log.info("[RETRY-DIAG] 모든 재시도 실패 - 전체 네트워크 진단 수행:");
            
            try {
                networkDiagnostics.performFullDiagnostics(targetUrl);
            } catch (Exception diagError) {
                log.info("[RETRY-DIAG] 네트워크 진단 중 오류: {}", diagError.getMessage());
            }
            
            throw e;
        }
    }
}