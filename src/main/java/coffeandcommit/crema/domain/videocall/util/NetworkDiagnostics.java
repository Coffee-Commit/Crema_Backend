package coffeandcommit.crema.domain.videocall.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.*;
import java.security.Security;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class NetworkDiagnostics {

    /**
     * DNS 조회를 수행하고 결과를 INFO 레벨로 로깅
     */
    public void performDnsLookup(String hostname) {
        log.info("[DNS-LOOKUP] ========== DNS 조회 시작 ==========");
        log.info("[DNS-LOOKUP] Target Host: {}", hostname);
        
        long startTime = System.currentTimeMillis();
        try {
            // 단일 주소 조회
            InetAddress singleAddress = InetAddress.getByName(hostname);
            log.info("[DNS-LOOKUP] Primary Address: {} -> {}", hostname, singleAddress.getHostAddress());
            
            // 모든 주소 조회
            InetAddress[] allAddresses = InetAddress.getAllByName(hostname);
            log.info("[DNS-LOOKUP] Total Addresses Found: {}", allAddresses.length);
            
            for (int i = 0; i < allAddresses.length; i++) {
                InetAddress addr = allAddresses[i];
                log.info("[DNS-LOOKUP]   Address[{}]: {} (Canonical: {})", 
                        i + 1, addr.getHostAddress(), addr.getCanonicalHostName());
            }
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("[DNS-LOOKUP] DNS 조회 성공 - 소요시간: {}ms", elapsedTime);
            
        } catch (UnknownHostException e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("[DNS-LOOKUP] DNS 조회 실패:");
            log.info("[DNS-LOOKUP]   - Host: {}", hostname);
            log.info("[DNS-LOOKUP]   - Error: {}", e.getMessage());
            log.info("[DNS-LOOKUP]   - 소요시간: {}ms", elapsedTime);
            log.info("[DNS-LOOKUP]   - 해결방법: DNS 서버 설정, /etc/hosts 파일, 네트워크 연결 확인");
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("[DNS-LOOKUP] DNS 조회 예외 발생:");
            log.info("[DNS-LOOKUP]   - Host: {}", hostname);
            log.info("[DNS-LOOKUP]   - Error Type: {}", e.getClass().getSimpleName());
            log.info("[DNS-LOOKUP]   - Error Message: {}", e.getMessage());
            log.info("[DNS-LOOKUP]   - 소요시간: {}ms", elapsedTime);
        }
    }

    /**
     * TCP 포트 연결 테스트를 수행하고 결과를 INFO 레벨로 로깅
     */
    public void performTcpConnectionTest(String hostname, int port, int timeoutMs) {
        log.info("[TCP-TEST] ========== TCP 연결 테스트 시작 ==========");
        log.info("[TCP-TEST] Target: {}:{}", hostname, port);
        log.info("[TCP-TEST] Timeout: {}ms", timeoutMs);
        
        long startTime = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(hostname, port), timeoutMs);
            long elapsedTime = System.currentTimeMillis() - startTime;
            
            log.info("[TCP-TEST] TCP 연결 성공:");
            log.info("[TCP-TEST]   - Remote Address: {}", socket.getRemoteSocketAddress());
            log.info("[TCP-TEST]   - Local Address: {}", socket.getLocalSocketAddress());
            log.info("[TCP-TEST]   - 연결 시간: {}ms", elapsedTime);
            log.info("[TCP-TEST]   - Keep Alive: {}", socket.getKeepAlive());
            log.info("[TCP-TEST]   - TCP No Delay: {}", socket.getTcpNoDelay());
            
        } catch (SocketTimeoutException e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("[TCP-TEST] TCP 연결 타임아웃:");
            log.info("[TCP-TEST]   - Host: {}:{}", hostname, port);
            log.info("[TCP-TEST]   - Timeout: {}ms", timeoutMs);
            log.info("[TCP-TEST]   - 실제 소요시간: {}ms", elapsedTime);
            log.info("[TCP-TEST]   - 원인: 방화벽, 서버 다운, 네트워크 지연");
            
        } catch (ConnectException e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("[TCP-TEST] TCP 연결 거부:");
            log.info("[TCP-TEST]   - Host: {}:{}", hostname, port);
            log.info("[TCP-TEST]   - Error: {}", e.getMessage());
            log.info("[TCP-TEST]   - 소요시간: {}ms", elapsedTime);
            log.info("[TCP-TEST]   - 원인: 서비스 중단, 포트 차단, 잘못된 포트 번호");
            
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("[TCP-TEST] TCP 연결 예외:");
            log.info("[TCP-TEST]   - Host: {}:{}", hostname, port);
            log.info("[TCP-TEST]   - Error Type: {}", e.getClass().getSimpleName());
            log.info("[TCP-TEST]   - Error Message: {}", e.getMessage());
            log.info("[TCP-TEST]   - 소요시간: {}ms", elapsedTime);
        }
    }

    /**
     * 네트워크 환경 정보를 INFO 레벨로 로깅
     */
    public void logNetworkEnvironment() {
        log.info("[NETWORK-ENV] ========== 네트워크 환경 정보 ==========");
        
        // JVM DNS 캐시 설정
        log.info("[NETWORK-ENV] JVM DNS 캐시 설정:");
        String positiveCacheTtl = Security.getProperty("networkaddress.cache.ttl");
        String negativeCacheTtl = Security.getProperty("networkaddress.cache.negative.ttl");
        log.info("[NETWORK-ENV]   - Positive Cache TTL: {} seconds", 
                positiveCacheTtl != null ? positiveCacheTtl : "default");
        log.info("[NETWORK-ENV]   - Negative Cache TTL: {} seconds", 
                negativeCacheTtl != null ? negativeCacheTtl : "default");
        
        // 시스템 프록시 설정
        log.info("[NETWORK-ENV] 시스템 프록시 설정:");
        String httpProxy = System.getProperty("http.proxyHost");
        String httpProxyPort = System.getProperty("http.proxyPort");
        String httpsProxy = System.getProperty("https.proxyHost");
        String httpsProxyPort = System.getProperty("https.proxyPort");
        String nonProxyHosts = System.getProperty("http.nonProxyHosts");
        
        log.info("[NETWORK-ENV]   - HTTP Proxy: {}", 
                httpProxy != null ? httpProxy + ":" + httpProxyPort : "없음");
        log.info("[NETWORK-ENV]   - HTTPS Proxy: {}", 
                httpsProxy != null ? httpsProxy + ":" + httpsProxyPort : "없음");
        log.info("[NETWORK-ENV]   - Non Proxy Hosts: {}", 
                nonProxyHosts != null ? nonProxyHosts : "없음");
        
        // 기타 네트워크 설정
        log.info("[NETWORK-ENV] 기타 네트워크 설정:");
        log.info("[NETWORK-ENV]   - IPv4 Stack: {}", 
                System.getProperty("java.net.preferIPv4Stack", "false"));
        log.info("[NETWORK-ENV]   - IPv6 Addresses: {}", 
                System.getProperty("java.net.preferIPv6Addresses", "false"));
        log.info("[NETWORK-ENV]   - Use System Proxies: {}", 
                System.getProperty("java.net.useSystemProxies", "false"));
        
        // 로컬 네트워크 인터페이스 정보
        logNetworkInterfaces();
    }

    /**
     * 네트워크 인터페이스 정보를 로깅
     */
    private void logNetworkInterfaces() {
        log.info("[NETWORK-ENV] 네트워크 인터페이스:");
        try {
            NetworkInterface.getNetworkInterfaces().asIterator().forEachRemaining(ni -> {
                try {
                    if (ni.isUp() && !ni.isLoopback()) {
                        log.info("[NETWORK-ENV]   - Interface: {} ({})", ni.getDisplayName(), ni.getName());
                        ni.getInterfaceAddresses().forEach(addr -> {
                            if (addr.getAddress() instanceof Inet4Address) {
                                log.info("[NETWORK-ENV]     IPv4: {}/{}", 
                                        addr.getAddress().getHostAddress(), 
                                        addr.getNetworkPrefixLength());
                            }
                        });
                    }
                } catch (SocketException e) {
                    log.info("[NETWORK-ENV]   - Interface error: {}", e.getMessage());
                }
            });
        } catch (SocketException e) {
            log.info("[NETWORK-ENV]   - 네트워크 인터페이스 조회 실패: {}", e.getMessage());
        }
    }

    /**
     * OpenVidu URL에서 호스트명을 추출
     */
    public String extractHostFromUrl(String url) {
        try {
            if (url == null || url.trim().isEmpty()) {
                return null;
            }
            
            // https:// 접두사 추가 (없는 경우)
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            
            URL parsedUrl = new URL(url);
            return parsedUrl.getHost();
        } catch (Exception e) {
            log.info("[URL-PARSE] URL 파싱 실패: {} - {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * OpenVidu URL에서 포트를 추출
     */
    public int extractPortFromUrl(String url) {
        try {
            if (url == null || url.trim().isEmpty()) {
                return -1;
            }
            
            // https:// 접두사 추가 (없는 경우)
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            
            URL parsedUrl = new URL(url);
            int port = parsedUrl.getPort();
            
            // 기본 포트 처리
            if (port == -1) {
                return "https".equals(parsedUrl.getProtocol()) ? 443 : 80;
            }
            
            return port;
        } catch (Exception e) {
            log.info("[URL-PARSE] 포트 추출 실패: {} - {}", url, e.getMessage());
            return -1;
        }
    }

    /**
     * 전체 네트워크 진단 수행
     */
    public void performFullDiagnostics(String openviduUrl) {
        log.info("[FULL-DIAGNOSTICS] ========== 전체 네트워크 진단 시작 ==========");
        log.info("[FULL-DIAGNOSTICS] Target URL: {}", openviduUrl);
        
        // 네트워크 환경 정보 출력
        logNetworkEnvironment();
        
        // URL 파싱
        String hostname = extractHostFromUrl(openviduUrl);
        int port = extractPortFromUrl(openviduUrl);
        
        if (hostname != null) {
            log.info("[FULL-DIAGNOSTICS] 파싱된 정보:");
            log.info("[FULL-DIAGNOSTICS]   - Hostname: {}", hostname);
            log.info("[FULL-DIAGNOSTICS]   - Port: {}", port);
            
            // DNS 조회
            performDnsLookup(hostname);
            
            // TCP 연결 테스트
            if (port > 0) {
                performTcpConnectionTest(hostname, port, 5000);
            }
        } else {
            log.info("[FULL-DIAGNOSTICS] URL 파싱 실패 - 진단 중단");
        }
        
        log.info("[FULL-DIAGNOSTICS] ========== 전체 네트워크 진단 완료 ==========");
    }
}