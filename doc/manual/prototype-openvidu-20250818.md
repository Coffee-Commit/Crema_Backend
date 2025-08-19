# OpenVidu 화상통화 프로토타입 구현 및 사용 가이드

## 📖 개요

이 문서는 Spring Boot와 OpenVidu를 이용해 구현한 화상통화 프로토타입의 사용법과 구현 내용을 설명합니다.

### 🎯 목적
- OpenVidu 기술의 검증 및 테스트
- 화상통화 기능의 기술적 구현 가능성 확인
- 실제 서비스 적용 전 프로토타입을 통한 사전 검증

### 📋 주요 기능
- 화상통화 세션 생성/참가/종료
- 실시간 비디오/오디오 스트리밍
- 참가자 간 실시간 채팅
- 비디오/오디오 ON/OFF 제어
- 반응형 웹 인터페이스

## 🏗️ 시스템 아키텍처

### 전체 구조도
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   웹 브라우저    │    │  Spring Boot    │    │  OpenVidu 서버   │
│   (클라이언트)   │    │    백엔드       │    │                │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │   HTTP REST API       │                       │
         ├──────────────────────►│                       │
         │                       │   OpenVidu Java SDK   │
         │                       ├──────────────────────►│
         │                       │                       │
         │       WebRTC         │                       │
         ├───────────────────────┼──────────────────────►│
         │                       │                       │
```

### 백엔드 컴포넌트
- **VideoCallController**: REST API 엔드포인트 제공
- **VideoCallService**: OpenVidu 서버와의 통신 및 비즈니스 로직
- **VideoSession**: 세션 정보 관리 엔티티
- **Participant**: 참가자 정보 관리 엔티티

### 프론트엔드 컴포넌트
- **index.html**: 세션 생성/참가 인터페이스
- **video-call.html**: 실제 화상통화 화면
- **JavaScript**: OpenVidu Browser SDK를 이용한 클라이언트 로직

## 🚀 설치 및 실행 방법

### 1. 사전 요구사항
- Java 21 이상
- Docker (OpenVidu 서버용)
- 웹브라우저 (Chrome, Firefox, Safari 등)

### 2. OpenVidu 서버 설치 및 실행

#### 방법 1: OpenVidu Cloud 사용 (추천)
1. [OpenVidu Cloud](https://openvidu.io/account)에서 계정 생성
2. 무료 티어로 시작 (월 50분 무료)
3. Dashboard에서 Application 생성
4. 제공되는 URL과 Secret을 application.yml에 설정

#### 방법 2: Docker를 이용한 로컬 설치
**주의**: Docker Desktop이 실행 중이어야 합니다.
```bash
# Docker Desktop 실행 확인 후
docker run -p 25565:4443 --rm -e OPENVIDU_SECRET=MY_SECRET openvidu/openvidu-dev:2.30.0
```

**Docker 실행 문제 해결**:
- Windows: Docker Desktop 애플리케이션 실행
- 시스템 트레이에서 Docker 아이콘이 안정화될 때까지 대기
- WSL2 업데이트 필요할 수 있음

### 3. 애플리케이션 설정

#### application.yml 설정
```yaml
openvidu:
  url: http://localhost:25565  # OpenVidu 서버 URL
  secret: MY_SECRET            # OpenVidu 서버 비밀키
```

#### 환경변수 설정 (선택사항)
```bash
export OPENVIDU_URL=http://localhost:25565
export OPENVIDU_SECRET=MY_SECRET
```

### 4. 애플리케이션 실행
```bash
# 1. 의존성 설치 및 빌드
./gradlew build

# 2. 애플리케이션 실행
./gradlew bootRun
```

## 🎮 사용 방법

### 1. 웹 애플리케이션 접속
브라우저에서 `http://localhost:8080` 접속

### 2. 세션 생성
1. "새 세션 생성" 섹션에서 세션 이름 입력
2. "세션 생성" 버튼 클릭
3. 생성된 세션 ID 확인

### 3. 세션 참가 (다른 브라우저 또는 사용자)
1. "기존 세션 참가" 섹션에서 세션 ID와 사용자명 입력
2. "세션 참가" 버튼 클릭

### 4. 화상통화 시작
1. "화상통화 시작" 버튼 클릭
2. 카메라/마이크 권한 허용
3. 화상통화 화면에서 다른 참가자와 통신

### 5. 화상통화 기능 사용
- **비디오 ON/OFF**: 📹 버튼 클릭
- **오디오 ON/OFF**: 🎤 버튼 클릭
- **채팅**: 우측 채팅창에서 메시지 입력
- **세션 나가기**: "나가기" 버튼 클릭

## 🔄 구현 플로우

### 1. 세션 생성 플로우
```
사용자 입력 → Spring Boot Controller → OpenVidu 서버
                ↓
        데이터베이스 저장 ← VideoCallService
                ↓
        세션 정보 반환 → 클라이언트
```

### 2. 세션 참가 플로우
```
세션 ID + 사용자명 → Controller → VideoCallService
                              ↓
                   OpenVidu 서버에서 토큰 생성
                              ↓
                   참가자 정보 데이터베이스 저장
                              ↓
                   토큰 반환 → 클라이언트
```

### 3. 실시간 통신 플로우
```
클라이언트 A                OpenVidu 서버               클라이언트 B
    │                          │                          │
    ├─ 토큰으로 세션 연결 ────────►│                          │
    │                          ├─ 스트림 생성 알림 ──────────►│
    │                          │                          │
    ├─ 비디오 스트림 발행 ────────►│                          │
    │                          ├─ 스트림 전달 ─────────────►│
    │                          │                          │
    │◄─ P2P 연결 설정 ──────────┼─ P2P 연결 설정 ──────────►│
    │                          │                          │
    │◄──────── 직접 통신 ───────────────────────────────────►│
```

## 📂 코드 구조

### 백엔드 구조
```
src/main/java/coffeandcommit/crema/
├── domain/videocall/
│   ├── controller/VideoCallController.java     # REST API 컨트롤러
│   ├── service/VideoCallService.java          # 비즈니스 로직
│   ├── entity/
│   │   ├── VideoSession.java                  # 세션 엔티티
│   │   └── Participant.java                   # 참가자 엔티티
│   ├── repository/
│   │   ├── VideoSessionRepository.java        # 세션 레포지토리
│   │   └── ParticipantRepository.java         # 참가자 레포지토리
│   ├── dto/
│   │   ├── request/                           # 요청 DTO
│   │   └── response/                          # 응답 DTO
│   └── exception/                             # 커스텀 예외
└── global/config/WebSocketConfig.java         # WebSocket 설정
```

### 프론트엔드 구조
```
src/main/resources/static/
├── index.html          # 메인 페이지
├── video-call.html     # 화상통화 페이지
├── styles.css          # CSS 스타일
├── app.js             # 메인 페이지 로직
└── video-call.js      # 화상통화 로직
```

## 🔧 API 명세

### 1. 세션 생성
- **URL**: `POST /api/video-call/sessions`
- **Request Body**:
  ```json
  {
    "sessionName": "테스트 세션"
  }
  ```
- **Response**:
  ```json
  {
    "id": 1,
    "sessionId": "session_1234567890",
    "sessionName": "테스트 세션",
    "createdAt": "2024-01-01T10:00:00",
    "isActive": true
  }
  ```

### 2. 세션 참가
- **URL**: `POST /api/video-call/sessions/{sessionId}/join`
- **Request Body**:
  ```json
  {
    "username": "사용자1"
  }
  ```
- **Response**:
  ```json
  {
    "token": "wss://localhost:4443/openvidu/...",
    "sessionId": "session_1234567890",
    "username": "사용자1"
  }
  ```

### 3. 세션 나가기
- **URL**: `DELETE /api/video-call/sessions/{sessionId}/leave?connectionId={connectionId}`
- **Response**: `200 OK`

### 4. 세션 종료
- **URL**: `DELETE /api/video-call/sessions/{sessionId}`
- **Response**: `200 OK`

### 5. 세션 정보 조회
- **URL**: `GET /api/video-call/sessions/{sessionId}`
- **Response**:
  ```json
  {
    "id": 1,
    "sessionId": "session_1234567890",
    "sessionName": "테스트 세션",
    "createdAt": "2024-01-01T10:00:00",
    "isActive": true
  }
  ```

## 🐛 트러블슈팅

### 1. OpenVidu 서버 연결 실패
**증상**: 세션 생성 시 "Connection refused" 오류
**해결방법**:
- OpenVidu 서버가 실행 중인지 확인
- application.yml의 URL과 Secret 값 확인
- 방화벽 설정 확인 (4443 포트)

### 2. 브라우저에서 카메라/마이크 접근 권한 오류
**증상**: 비디오/오디오 스트림이 표시되지 않음
**해결방법**:
- HTTPS 환경에서 실행 (프로덕션)
- 브라우저에서 카메라/마이크 권한 허용
- 다른 애플리케이션이 카메라/마이크를 사용 중인지 확인

### 3. 다중 사용자 연결 시 비디오가 보이지 않음
**증상**: 한 명의 비디오만 표시됨
**해결방법**:
- 네트워크 방화벽 설정 확인
- WebRTC STUN/TURN 서버 설정 확인
- 브라우저 개발자 도구에서 네트워크 오류 확인

### 4. 채팅 메시지가 전송되지 않음
**증상**: 채팅 입력 후 전송되지 않음
**해결방법**:
- WebSocket 연결 상태 확인
- 브라우저 콘솔에서 JavaScript 오류 확인
- OpenVidu 세션 연결 상태 확인

## 🧪 테스트 시나리오

### 기본 테스트
1. **단일 사용자 테스트**
   - 세션 생성 → 참가 → 로컬 비디오 확인

2. **다중 사용자 테스트**
   - 사용자 A: 세션 생성 및 참가
   - 사용자 B: 동일 세션 참가
   - 양방향 비디오/오디오 통신 확인

3. **채팅 기능 테스트**
   - 실시간 메시지 전송/수신 확인
   - 참가자 입장/퇴장 알림 확인

### 고급 테스트
1. **네트워크 불안정 상황**
   - 네트워크 지연 시뮬레이션
   - 연결 재시도 확인

2. **다양한 브라우저 테스트**
   - Chrome, Firefox, Safari 등에서 테스트
   - 모바일 브라우저 테스트

3. **동시 다중 세션 테스트**
   - 여러 세션 동시 실행
   - 서버 리소스 사용량 모니터링

## 🚀 프로덕션 배포 고려사항

### 1. 보안 설정
- HTTPS 필수 (WebRTC 요구사항)
- OpenVidu 서버 보안 설정
- 세션 접근 권한 관리

### 2. 성능 최적화
- OpenVidu 서버 스케일링
- CDN을 통한 정적 자원 배포
- 데이터베이스 최적화

### 3. 모니터링
- OpenVidu 서버 상태 모니터링
- 애플리케이션 로그 관리
- 사용자 연결 상태 추적

## 📝 추가 개발 아이템

### 단기 개선사항
- [ ] 사용자 인증 시스템 연동
- [ ] 세션 녹화 기능
- [ ] 화면 공유 기능
- [ ] 참가자 목록 UI 개선

### 중장기 개선사항
- [ ] 모바일 앱 개발
- [ ] 클라우드 배포 및 스케일링
- [ ] 고급 채팅 기능 (파일 전송, 이모지)
- [ ] 화질 조절 및 네트워크 적응 기능

## 📞 지원 및 문의

### 관련 문서
- [OpenVidu 공식 문서](https://docs.openvidu.io/)
- [Spring Boot 문서](https://spring.io/projects/spring-boot)
- [WebRTC 개발 가이드](https://webrtc.org/getting-started/overview)

### 개발팀 연락처
- 프로젝트 저장소: [GitHub Repository]
- 이슈 리포팅: [GitHub Issues]

---

**작성일**: 2025년 8월 18일  
**버전**: v1.0.0  
**작성자**: Crema 개발팀