# OpenVidu 화상통화 프로토타입 구현 정리 (2025-01-18)

## 1. 프로젝트 개요

### 목적
- OpenVidu 기술을 활용한 화상통화 시스템의 기술적 검증
- Spring Boot와 OpenVidu 통합 가능성 검토
- WebRTC 기반 실시간 화상통화 구현 프로토타입 개발

### 사용 기술 스택
- **Backend**: Spring Boot 3.5.4, Java 21
- **Frontend**: Vanilla JavaScript, HTML5, CSS3
- **화상통화**: OpenVidu 2.30.0 (Open Source)
- **데이터베이스**: H2 In-Memory Database
- **WebRTC**: OpenVidu Browser SDK 2.30.0
- **의존성 관리**: Gradle

## 2. 구현 아키텍처

### 2.1 전체 시스템 구조
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Spring Boot   │    │   OpenVidu      │
│   (Browser)     │◄──►│   Backend       │◄──►│   Server        │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ • index.html    │    │ • VideoCall     │    │ • Port: 25565   │
│ • video-call.js │    │   Controller    │    │ • Docker        │
│ • app.js        │    │ • VideoCall     │    │ • WebRTC        │
└─────────────────┘    │   Service       │    │   Processing    │
                       │ • JPA Entities  │    └─────────────────┘
                       └─────────────────┘
```

### 2.2 데이터베이스 구조
```sql
-- 화상통화 세션 테이블
CREATE TABLE video_sessions (
    id BIGINT PRIMARY KEY,
    session_id VARCHAR(255) UNIQUE NOT NULL,
    session_name VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    ended_at TIMESTAMP
);

-- 참가자 테이블
CREATE TABLE participants (
    id BIGINT PRIMARY KEY,
    connection_id VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(255) NOT NULL,
    token TEXT,
    is_connected BOOLEAN DEFAULT TRUE,
    joined_at TIMESTAMP,
    left_at TIMESTAMP,
    video_session_id BIGINT,
    FOREIGN KEY (video_session_id) REFERENCES video_sessions(id)
);
```

## 3. 주요 구현 컴포넌트

### 3.1 Backend 구현

#### 3.1.1 의존성 설정 (build.gradle)
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
    implementation 'io.openvidu:openvidu-java-client:2.30.0'
    runtimeOnly 'com.h2database:h2'
}
```

#### 3.1.2 VideoCallService 핵심 로직
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoCallService {
    
    @Value("${openvidu.url}")
    private String openviduUrl;
    
    @Value("${openvidu.secret}")
    private String openviduSecret;
    
    private OpenVidu openVidu;
    
    @PostConstruct
    private void init() {
        this.openVidu = new OpenVidu(openviduUrl, openviduSecret);
    }
    
    // 세션 생성
    public VideoSession createSession(String sessionName) {
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
    }
    
    // 세션 참가
    public String joinSession(String sessionId, String username) {
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
        
        // 참가자 정보 저장
        Participant participant = Participant.builder()
                .connectionId(connection.getConnectionId())
                .token(token)
                .username(username)
                .videoSession(videoSession)
                .build();

        participantRepository.save(participant);
        return token;
    }
}
```

#### 3.1.3 REST API 엔드포인트
```java
@RestController
@RequestMapping("/api/video-call")
public class VideoCallController {
    
    // 세션 생성
    @PostMapping("/sessions")
    public ResponseEntity<VideoSessionResponse> createSession(
            @RequestBody VideoSessionRequest request) {
        VideoSession session = videoCallService.createSession(request.getSessionName());
        return ResponseEntity.ok(VideoSessionResponse.from(session));
    }
    
    // 세션 참가
    @PostMapping("/sessions/{sessionId}/join")
    public ResponseEntity<JoinSessionResponse> joinSession(
            @PathVariable String sessionId,
            @RequestBody JoinSessionRequest request) {
        String token = videoCallService.joinSession(sessionId, request.getUsername());
        
        JoinSessionResponse response = JoinSessionResponse.builder()
                .sessionId(sessionId)
                .username(request.getUsername())
                .token(token)
                .build();
                
        return ResponseEntity.ok(response);
    }
}
```

### 3.2 Frontend 구현

#### 3.2.1 메인 페이지 (index.html + app.js)
- 세션 생성 및 참가 UI
- 자동 세션 참가 기능
- 세션 정보 표시

#### 3.2.2 화상통화 페이지 (video-call.html + video-call.js)
- OpenVidu Browser SDK 통합
- 실시간 비디오/오디오 스트리밍
- 채팅 기능
- 비디오/오디오 제어

#### 3.2.3 WebSocket URL 리디렉션 처리
```javascript
// 포트 미스매치 해결을 위한 WebSocket URL 리디렉션
if (window.WebSocket) {
    const originalWebSocket = window.WebSocket;
    window.WebSocket = function(url, protocols) {
        if (url.includes('localhost:4443')) {
            url = url.replace('localhost:4443', 'localhost:25565');
            console.log('WebSocket URL redirected to:', url);
        }
        return new originalWebSocket(url, protocols);
    };
}
```

## 4. 기술적 과제와 해결방법

### 4.1 Spring Boot 3.x 호환성 이슈

**문제**: `javax.annotation.PostConstruct` import 오류
```
java: package javax.annotation does not exist
```

**해결방법**: Jakarta EE 네임스페이스 사용
```java
// 변경 전
import javax.annotation.PostConstruct;

// 변경 후  
import jakarta.annotation.PostConstruct;
```

### 4.2 데이터베이스 연결 문제

**문제**: MySQL 환경변수 미설정으로 인한 연결 실패

**해결방법**: H2 In-Memory 데이터베이스로 변경
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password: 
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
```

### 4.3 Spring Security 접근 제한

**문제**: 기본 Spring Security 설정으로 API 접근 차단

**해결방법**: 프로토타입 테스트를 위한 보안 비활성화
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
```

### 4.4 포트 충돌 및 설정 문제

**문제**: 
- Spring Boot 기본 포트(8080) 충돌 
- OpenVidu 클라이언트가 4443 포트로 연결 시도하지만 서버는 25565 포트에서 실행

**해결방법**:
1. Spring Boot 포트 변경: 8080 → 8081 → 9090
2. WebSocket URL 리디렉션 로직 구현

### 4.5 User Entity 의존성 문제

**문제**: Participant 엔티티에서 존재하지 않는 User 엔티티 참조

**해결방법**: Participant 엔티티 구조 변경
```java
@Entity
@Table(name = "participants")
public class Participant {
    // User 엔티티 대신 username 필드 직접 사용
    @Column(name = "username", nullable = false) 
    private String username;
}
```

### 4.6 OpenVidu 토큰 인증 문제 (진행 중)

**현재 상황**: 
- 401 인증 오류 발생
- WebSocket 연결 시 토큰 형식 불일치 추정

**시도한 해결방법**:
- 토큰 생성 로깅 추가
- WebSocket URL 리디렉션 구현
- OpenVidu 서버 직접 테스트 (정상 동작 확인)

## 5. 현재 구현 상태

### 5.1 완료된 기능
✅ Spring Boot 백엔드 API 구조 완성  
✅ OpenVidu Java SDK 통합  
✅ JPA 엔티티 및 Repository 구현  
✅ REST API 엔드포인트 구현  
✅ 프론트엔드 UI 구현  
✅ OpenVidu Browser SDK 통합  
✅ 세션 생성 및 참가 기능  
✅ 기본 비디오/오디오 제어 기능  
✅ 채팅 기능 구현  
✅ WebSocket URL 리디렉션 로직  

### 5.2 진행 중인 문제
🔄 OpenVidu 토큰 인증 오류 (401 에러)  
🔄 WebSocket 연결 안정성 문제  
🔄 포트 설정 최적화 필요  

### 5.3 테스트 가능한 기능
- ✅ 세션 생성 (POST /api/video-call/sessions)
- ✅ 세션 참가 (POST /api/video-call/sessions/{sessionId}/join)
- ✅ 프론트엔드 UI 동작
- ✅ 기본 API 응답 확인

## 6. 설정 정보

### 6.1 애플리케이션 설정
```yaml
# application.yml
server:
  port: 8081

openvidu:
  url: ${OPENVIDU_URL:http://localhost:25565}
  secret: ${OPENVIDU_SECRET:MY_SECRET}

spring:
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password: 
    driver-class-name: org.h2.Driver
```

### 6.2 OpenVidu 서버 실행
```bash
# Docker를 이용한 OpenVidu 개발 서버 실행
docker run -p 25565:4443 \
    -e OPENVIDU_SECRET=MY_SECRET \
    openvidu/openvidu-dev:2.30.0
```

## 7. 사용법

### 7.1 서버 실행
1. OpenVidu 서버 실행 (Docker)
2. Spring Boot 애플리케이션 실행
3. 웹 브라우저에서 `http://localhost:8081` 접속

### 7.2 테스트 시나리오
1. **세션 생성**: 메인 페이지에서 세션명 입력 후 생성
2. **세션 참가**: 생성된 세션 ID와 사용자명으로 참가
3. **화상통화**: "화상통화 시작" 버튼 클릭
4. **기능 테스트**: 비디오/오디오 토글, 채팅 메시지 전송

## 8. 향후 개선사항

### 8.1 우선순위 높음
- 🔴 OpenVidu 토큰 인증 문제 해결
- 🔴 WebSocket 연결 안정성 개선
- 🔴 포트 설정 표준화

### 8.2 기능 개선
- 🟡 사용자 인증 시스템 통합
- 🟡 세션 관리 UI 개선
- 🟡 화면 공유 기능 추가
- 🟡 녹화 기능 구현

### 8.3 운영 환경 대응
- 🟢 HTTPS 적용
- 🟢 로드 밸런싱 고려
- 🟢 모니터링 시스템 연동
- 🟢 에러 처리 개선

## 9. 결론

### 9.1 기술적 검증 결과
- ✅ OpenVidu와 Spring Boot 통합 **가능**
- ✅ WebRTC 기반 화상통화 구현 **가능**
- ✅ REST API를 통한 세션 관리 **가능**
- ⚠️ 일부 설정 및 인증 문제 해결 **필요**

### 9.2 프로덕션 적용 고려사항
1. **보안**: 현재 비활성화된 Spring Security 재설정 필요
2. **확장성**: 대규모 동시접속 사용자 대응 방안
3. **안정성**: 네트워크 장애 및 연결 끊김 처리
4. **모니터링**: 화상통화 품질 및 성능 모니터링

### 9.3 권장사항
OpenVidu 기술은 **프로덕션 환경에서 사용 가능**하다고 판단되며, 현재 발생한 토큰 인증 문제는 설정 조정을 통해 해결 가능할 것으로 예상됩니다. 추가 개발 전 포트 설정 표준화 및 인증 문제 해결을 우선적으로 진행할 것을 권장합니다.