# OpenVidu React 프론트엔드 구현 매뉴얼

## 📋 프로젝트 개요 및 개발자 가이드

### 🎯 프로젝트 목적
이 문서는 **프론트엔드 개발자**를 위한 OpenVidu 화상통화 시스템 React 구현 가이드입니다. Vue.js로 구현된 프로토타입을 참고하여 **React 애플리케이션으로 화상통화 기능을 구현**하는 것이 목표입니다.

### 🤔 프로젝트 배경은?

**현재 상황:**
- ✅ Spring Boot 백엔드와 OpenVidu 서버 연동 완료
- ✅ Vue.js로 기본 화상통화 기능 프로토타입 구현 완료
- ✅ 기술적 검증 완료 - OpenVidu 기술 사용 가능성 확인
- 🎯 이제 React 환경에서 실제 서비스용 화상통화 기능 구현 필요

**React 구현의 목표:**
- 🚀 컴포넌트 기반 모듈화된 구조
- 🎯 예측 가능한 상태 관리
- 🔧 개발 생산성 향상
- 📱 확장성과 유지보수성 개선
- ⚡ **백엔드 고급 API 활용으로 개발 시간 70% 단축**

### 🎥 OpenVidu가 무엇인가요?

OpenVidu는 **WebRTC 기반의 실시간 화상통화 플랫폼**입니다:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   사용자 A      │    │   OpenVidu      │    │   사용자 B      │
│   (React App)   │◄──►│   Server        │◄──►│   (React App)   │
│                 │    │                 │    │                 │
│ • 비디오 전송   │    │ • 중계 서버     │    │ • 비디오 수신   │
│ • 오디오 전송   │    │ • 세션 관리     │    │ • 오디오 수신   │
│ • 채팅 메시지   │    │ • 미디어 처리   │    │ • 채팅 참여     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 🏗️ 전체 시스템 아키텍처

```
프론트엔드 (React)          백엔드 (Spring Boot)      OpenVidu 서버
┌─────────────────┐        ┌─────────────────┐      ┌─────────────────┐
│                 │        │                 │      │                 │
│ Home Page       │   ➤    │ VideoCall       │  ➤   │ Session         │
│ • 세션 생성     │  HTTP  │ Controller      │ HTTP │ Management      │
│ • 세션 참가     │   API  │                 │ API  │                 │
│                 │        │ VideoCall       │      │                 │
│ VideoCall Page  │        │ Service         │      │ Media           │
│ • 실시간 비디오 │◄─────  │                 │      │ Processing      │
│ • 오디오 제어   │ WebRTC │ OpenVidu        │      │                 │
│ • 채팅 기능     │        │ Integration     │      │                 │
└─────────────────┘        └─────────────────┘      └─────────────────┘
```

### 👨‍💻 프론트엔드 개발자의 역할과 책임

#### 🎯 **주요 책임 범위 (대폭 간소화됨)**

1. **React 애플리케이션 구조 설계**
   - 컴포넌트 계층 구조 설계
   - 기본 상태 관리 (복잡한 OpenVidu 상태는 백엔드 처리)
   - 라우팅 및 네비게이션 구현

2. **간소화된 OpenVidu 연동** ⚡
   - ~~복잡한 OpenVidu JavaScript API 설정~~ → **단순 연결만**
   - ~~WebRTC 연결 관리~~ → **백엔드에서 완성된 토큰 받아서 연결**
   - ~~미디어 스트림 제어 로직~~ → **기본 on/off 컨트롤만**

3. **사용자 인터페이스 구현** (변경 없음)
   - 화상통화 UI/UX 설계
   - 반응형 디자인 구현
   - 접근성 고려사항 적용

4. **간소화된 백엔드 API 연동** ⚡
   - ~~복잡한 다중 REST API 호출~~ → **원클릭 API 1-2개만 호출**
   - ~~복잡한 에러 처리~~ → **백엔드 자동 처리, 간단한 UI 표시만**
   - ~~복잡한 상태 관리~~ → **간단한 로딩/성공/실패 상태만**

#### ✨ **대폭 줄어든 작업 (이제 신경쓰지 않아도 됨)**
- ✅ ~~OpenVidu 서버 설정 및 URL 관리~~ → **백엔드에서 자동 제공**
- ✅ ~~토큰 생성, 갱신, 만료 처리~~ → **백엔드에서 자동 처리**
- ✅ ~~WebSocket URL 리디렉션~~ → **백엔드에서 자동 설정**
- ✅ ~~세션 생성 → 참가 → 연결의 복잡한 플로우~~ → **원클릭 API로 간소화**
- ✅ ~~네트워크 오류 시 재연결 로직~~ → **백엔드 자동 재연결 API**
- ✅ ~~OpenVidu 이벤트 복잡한 에러 처리~~ → **백엔드에서 안정성 보장**

#### 🚫 **여전히 담당하지 않는 영역**
- ❌ Spring Boot 백엔드 개발
- ❌ OpenVidu 서버 설정 및 관리
- ❌ 데이터베이스 스키마 설계
- ❌ 서버 인프라 구성

#### 🎯 **이제 프론트엔드 개발자가 집중해야 할 핵심 영역**
1. **사용자 경험(UX) 설계** - 직관적이고 사용하기 쉬운 화상통화 인터페이스
2. **반응형 디자인** - 다양한 디바이스에서 최적화된 화상통화 경험
3. **접근성** - 모든 사용자가 접근 가능한 화상통화 서비스
4. **성능 최적화** - 부드럽고 빠른 UI 상호작용

### 🔧 필요한 기술적 배경 지식

#### ✅ **꼭 필요한 React 개념들**
- **기본 Hooks**: useState, useEffect, useRef
- **이벤트 처리**: 사용자 인터랙션 (클릭, 입력 등)
- **비동기 처리**: fetch API, async/await, 기본 에러 핸들링
- **컴포넌트 구조**: props, 조건부 렌더링

#### 📚 **있으면 좋은 개념들 (필수 아님)**
- **고급 Hooks**: useCallback, useMemo (성능 최적화 시)
- **상태 관리**: 전역 상태 관리 (복잡한 앱인 경우)
- **WebRTC 기초**: 단순 이해 수준 (상세한 구현은 백엔드에서 처리)

#### ⚡ **더 이상 알 필요 없는 개념들 (백엔드에서 처리)**
- ~~**OpenVidu 복잡한 API**: 세션 생성, 토큰 관리, 연결 설정~~
- ~~**WebSocket 고급 설정**: URL 리디렉션, 프로토콜 처리~~
- ~~**복잡한 에러 복구**: 재연결 로직, 토큰 갱신~~
- ~~**미디어 스트림 세부 제어**: 코덱, 해상도, 네트워크 최적화~~

### 🎯 구현해야 할 핵심 기능들

#### 🚀 **백엔드 고급 API를 활용한 간소화된 구현**

이제 복잡한 OpenVidu 설정이나 토큰 관리는 백엔드에서 처리하므로, 프론트엔드 개발자는 **UI/UX에만 집중**할 수 있습니다!

#### 🏠 **1. 홈 페이지 (간소화된 세션 관리)**
```javascript
// ⚡ 원클릭 참가 - 모든 복잡한 로직이 백엔드에서 처리됨
const HomePage = () => {
  const quickJoinSession = async (sessionName, username) => {
    try {
      const response = await fetch('/api/video-call/advanced/quick-join', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          sessionName: sessionName,
          username: username,
          autoCreateSession: true  // 세션이 없으면 자동 생성
        })
      });
      
      const sessionData = await response.json();
      
      // 세션 데이터 저장 후 화상통화 페이지로 이동
      localStorage.setItem('videoCallSession', JSON.stringify(sessionData));
      navigate('/video-call');
      
    } catch (error) {
      console.error('세션 참가 실패:', error);
    }
  };
};
```

#### 📹 **2. 화상통화 페이지 (대폭 간소화됨)**
```javascript
// ⚡ 백엔드에서 완성된 설정을 받아 바로 연결
const VideoCallPage = () => {
  const connectToSession = async (sessionData) => {
    // 1. OpenVidu 초기화 (설정 자동 적용)
    const OV = new OpenVidu();
    const session = OV.initSession();
    
    // 2. 간단한 이벤트 리스너만 설정
    session.on('streamCreated', handleStreamCreated);
    session.on('streamDestroyed', handleStreamDestroyed);
    session.on('signal:chat', handleChatMessage);
    
    // 3. 백엔드에서 받은 토큰으로 바로 연결
    await session.connect(sessionData.token, { username: sessionData.username });
    
    // 4. Publisher 생성 및 발행
    const publisher = await OV.initPublisher();
    await session.publish(publisher);
  };
  
  // 비디오/오디오 제어는 기존과 동일 (간단함)
  const toggleVideo = () => publisher.publishVideo(!videoEnabled);
  const toggleAudio = () => publisher.publishAudio(!audioEnabled);
};
```

#### 🔄 **3. 자동 에러 복구 (새로운 기능)**
```javascript
// ⚡ 네트워크 오류 시 자동 재연결 - 복잡한 로직 없이 API 호출만
const handleConnectionError = async () => {
  try {
    const response = await fetch(`/api/video-call/advanced/sessions/${sessionId}/auto-reconnect`, {
      method: 'POST',
      body: new URLSearchParams({
        username: currentUser,
        lastConnectionId: lastConnectionId
      })
    });
    
    const reconnectData = await response.json();
    
    // 새 토큰으로 자동 재연결
    await session.connect(reconnectData.token);
    
  } catch (error) {
    console.error('재연결 실패:', error);
  }
};

// ⚡ 토큰 자동 갱신 (토큰 만료 걱정 없음)
const refreshToken = async () => {
  const response = await fetch(`/api/video-call/advanced/sessions/${sessionId}/refresh-token?username=${username}`, {
    method: 'POST'
  });
  
  const newTokenData = await response.json();
  return newTokenData.token;
};
```

#### 📊 **4. 실시간 세션 모니터링 (새로운 기능)**
```javascript
// ⚡ 세션 상태 실시간 조회 - 복잡한 상태 관리 없이 API 호출만
const getSessionStatus = async () => {
  const response = await fetch(`/api/video-call/advanced/sessions/${sessionId}/status`);
  const status = await response.json();
  
  // 참가자 수, 연결 상태 등 모든 정보가 정리되어 제공됨
  updateParticipantList(status.participants);
  updateConnectionStatus(status.isActive);
};
```

### 🛠️ 개발 환경 및 도구

#### 📦 **최소한의 필수 의존성**
```json
{
  "dependencies": {
    "react": "^18.x",
    "openvidu-browser": "^2.30.0",  // OpenVidu 클라이언트 SDK (간단한 연결만 사용)
    "react-router-dom": "^6.x"      // 라우팅
  }
}
```

#### 📦 **선택적 의존성 (필요시 추가)**
```json
{
  "optionalDependencies": {
    "axios": "^1.x",                // HTTP 클라이언트 (fetch 대신 사용 시)
    "zustand": "^4.x",              // 상태 관리 (복잡한 상태 필요시)
    "@mui/material": "^5.x"         // UI 컴포넌트 라이브러리 (빠른 UI 구성시)
  }
}
```

#### 🎨 **스타일링 도구 (간소화됨)**
- **CSS Modules**: 기본 스타일링 (가장 간단)
- **SCSS**: 변수 사용 시 (선택사항)
- **CSS Grid/Flexbox**: 반응형 레이아웃

### ⚠️ 주요 고려사항 및 제약사항

#### 🔒 **보안 및 권한 (간소화됨)**
- **HTTPS 필수**: WebRTC는 보안 컨텍스트에서만 동작
- **미디어 권한**: 카메라/마이크 권한 요청 처리 (브라우저 기본 기능 활용)
- ~~**CORS 설정**: 백엔드 API 호출 시 CORS 정책 확인~~ → **백엔드에서 미리 설정**

#### 🌐 **브라우저 호환성 (걱정 덜어짐)**
- **모던 브라우저**: Chrome, Firefox, Safari, Edge 최신 버전
- **모바일 브라우저**: iOS Safari, Android Chrome 지원
- ~~**WebRTC API 차이 처리**~~ → **백엔드에서 표준화된 설정 제공**

#### 📱 **성능 최적화 (간소화됨)**
- **기본 컴포넌트 최적화**: 과도한 리렌더링 방지
- ~~**복잡한 메모리 관리**~~ → **백엔드에서 연결 생명주기 관리**
- ~~**복잡한 네트워크 최적화**~~ → **백엔드에서 자동 처리**

#### ✨ **이제 프론트엔드에서 신경쓰지 않아도 되는 것들**
- ✅ OpenVidu 서버 연결 상태 모니터링
- ✅ 토큰 만료 및 갱신 타이밍
- ✅ 네트워크 오류 시 재연결 로직
- ✅ WebSocket 프로토콜 호환성
- ✅ 미디어 코덱 최적화

### 🎯 성공적인 구현을 위한 단계별 접근법

#### ⚡ **대폭 단축된 개발 일정 (기존 7-10일 → 2-3일)**

#### 📅 **1단계: 기본 설정 (0.5일)**
- React 프로젝트 생성 및 의존성 설치
- 환경 변수 설정 (백엔드 API URL만)
- 기본 라우팅 구조 생성

#### 📅 **2단계: 고급 API 연동 (0.5일)**
- 원클릭 참가 API 연동
- 간단한 에러 처리 구현
- 설정 정보 자동 가져오기

#### 📅 **3단계: OpenVidu 연결 (0.5일)**
- ⚡ 백엔드에서 제공하는 완성된 설정으로 즉시 연결
- 기본 세션 연결 (복잡한 설정 불필요)
- 비디오 스트림 표시

#### 📅 **4단계: UI/UX 구현 (1일)**
- 화상통화 인터페이스 구현
- 비디오/오디오 컨트롤
- 채팅 기능 추가
- 실시간 세션 상태 표시

#### 📅 **5단계: 고급 기능 (0.5일)**
- 자동 재연결 기능 적용
- 토큰 자동 갱신 적용
- 최종 테스트

### 💡 개발 팁 및 베스트 프랙티스

#### 🔍 **디버깅 전략**
```javascript
// OpenVidu 연결 상태 모니터링
console.log('OpenVidu Session State:', session.connection?.connectionId);
console.log('Publisher State:', publisher?.stream?.streamId);
console.log('Subscribers Count:', subscribers.length);
```

#### 🎯 **상태 관리 패턴**
```javascript
// Zustand를 활용한 상태 관리 예시
const useVideoCallStore = create((set, get) => ({
  sessionData: null,
  isConnected: false,
  participants: [],
  
  // 액션들
  setSessionData: (data) => set({ sessionData: data }),
  addParticipant: (participant) => set((state) => ({
    participants: [...state.participants, participant]
  }))
}));
```

#### 🚀 **성능 최적화 팁**
```javascript
// 비디오 컴포넌트 최적화
const VideoComponent = React.memo(({ stream, username }) => {
  // 불필요한 리렌더링 방지
});

// 이벤트 핸들러 최적화
const handleVideoToggle = useCallback(() => {
  // 의존성 배열로 불필요한 함수 재생성 방지
}, [publisher]);
```

---

## 목차
1. [프로젝트 설정](#1-프로젝트-설정)
2. [의존성 설치](#2-의존성-설치)
3. [프로젝트 구조 설계](#3-프로젝트-구조-설계)
4. [환경 설정](#4-환경-설정)
5. [API 서비스 구현](#5-api-서비스-구현)
6. [React 컴포넌트 구현](#6-react-컴포넌트-구현)
7. [OpenVidu 통합](#7-openvidu-통합)
8. [스타일링](#8-스타일링)
9. [테스트 및 디버깅](#9-테스트-및-디버깅)
10. [배포 준비](#10-배포-준비)

---

## 1. 프로젝트 설정

### 1.1 React 프로젝트 생성

```bash
# 새로운 React 프로젝트 생성
npx create-react-app openvidu-react-app
cd openvidu-react-app

# 또는 Vite 사용 (권장)
npm create vite@latest openvidu-react-app -- --template react
cd openvidu-react-app
npm install
```

### 1.2 TypeScript 설정 (선택사항)

```bash
# TypeScript 의존성 추가
npm install --save-dev typescript @types/react @types/react-dom

# tsconfig.json 생성
npx tsc --init
```

### 1.3 기본 프로젝트 정리

```bash
# 불필요한 파일 제거
rm src/App.test.js src/logo.svg src/reportWebVitals.js src/setupTests.js
```

---

## 2. 의존성 설치

### 2.1 필수 의존성

```bash
# OpenVidu Browser SDK
npm install openvidu-browser

# HTTP 클라이언트
npm install axios

# 상태 관리 (선택)
npm install zustand
# 또는 Redux Toolkit
# npm install @reduxjs/toolkit react-redux

# UI 라이브러리 (선택)
npm install @mui/material @emotion/react @emotion/styled
# 또는 Ant Design
# npm install antd

# 유틸리티
npm install classnames
npm install react-router-dom
```

### 2.2 개발 의존성

```bash
# 스타일링
npm install --save-dev sass

# 아이콘
npm install react-icons
```

---

## 3. 프로젝트 구조 설계

### 3.1 폴더 구조

```
src/
├── components/
│   ├── common/
│   │   ├── Button/
│   │   ├── Input/
│   │   └── Loading/
│   ├── layout/
│   │   ├── Header/
│   │   └── Container/
│   └── video/
│       ├── VideoSession/
│       ├── VideoControls/
│       ├── VideoGrid/
│       ├── LocalVideo/
│       ├── RemoteVideo/
│       └── Chat/
├── pages/
│   ├── Home/
│   ├── VideoCall/
│   └── SessionList/
├── services/
│   ├── api/
│   ├── openvidu/
│   └── storage/
├── hooks/
│   ├── useVideoCall/
│   ├── useOpenVidu/
│   └── useSocket/
├── store/
│   ├── videoCallStore.js
│   └── userStore.js
├── utils/
│   ├── constants.js
│   ├── helpers.js
│   └── validation.js
├── styles/
│   ├── globals.scss
│   ├── variables.scss
│   └── components/
└── types/ (TypeScript 사용 시)
    ├── api.ts
    ├── openvidu.ts
    └── common.ts
```

### 3.2 컴포넌트 설계 원칙

```javascript
// 컴포넌트 구조 예시
const VideoCallComponent = {
  // 1. 상태 관리 (useState, useReducer)
  // 2. 부수 효과 (useEffect)
  // 3. 커스텀 훅 사용
  // 4. 이벤트 핸들러
  // 5. 렌더링 로직
};
```

---

## 4. 환경 설정

### 4.1 환경 변수 설정

**`.env` 파일 생성:**

```env
# OpenVidu 설정
REACT_APP_OPENVIDU_SERVER_URL=http://localhost:25565
REACT_APP_OPENVIDU_SERVER_SECRET=MY_SECRET

# Backend API 설정
REACT_APP_API_BASE_URL=http://localhost:8081
REACT_APP_API_ENDPOINT=/api/video-call

# 개발 환경 설정
REACT_APP_ENVIRONMENT=development
REACT_APP_DEBUG_MODE=true
```

### 4.2 상수 파일 생성

**`src/utils/constants.js`:**

```javascript
export const API_CONFIG = {
  BASE_URL: process.env.REACT_APP_API_BASE_URL || 'http://localhost:8081',
  ENDPOINTS: {
    SESSIONS: '/api/video-call/sessions',
    JOIN_SESSION: (sessionId) => `/api/video-call/sessions/${sessionId}/join`,
    LEAVE_SESSION: (sessionId, connectionId) => 
      `/api/video-call/sessions/${sessionId}/leave/${connectionId}`,
    END_SESSION: (sessionId) => `/api/video-call/sessions/${sessionId}/end`
  }
};

export const OPENVIDU_CONFIG = {
  SERVER_URL: process.env.REACT_APP_OPENVIDU_SERVER_URL || 'http://localhost:25565',
  SERVER_SECRET: process.env.REACT_APP_OPENVIDU_SERVER_SECRET || 'MY_SECRET'
};

export const VIDEO_CONFIG = {
  RESOLUTION: '640x480',
  FRAME_RATE: 30,
  AUDIO_ENABLED: true,
  VIDEO_ENABLED: true
};

export const CHAT_CONFIG = {
  MAX_MESSAGE_LENGTH: 500,
  MESSAGE_TYPES: {
    CHAT: 'chat',
    SYSTEM: 'system'
  }
};
```

---

## 5. API 서비스 구현

### 5.1 HTTP 클라이언트 설정

**`src/services/api/client.js`:**

```javascript
import axios from 'axios';
import { API_CONFIG } from '../../utils/constants';

const apiClient = axios.create({
  baseURL: API_CONFIG.BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  }
});

// 요청 인터셉터
apiClient.interceptors.request.use(
  (config) => {
    console.log('API Request:', config.method?.toUpperCase(), config.url);
    return config;
  },
  (error) => {
    console.error('API Request Error:', error);
    return Promise.reject(error);
  }
);

// 응답 인터셉터
apiClient.interceptors.response.use(
  (response) => {
    console.log('API Response:', response.status, response.config.url);
    return response;
  },
  (error) => {
    console.error('API Response Error:', {
      status: error.response?.status,
      message: error.response?.data?.message || error.message,
      url: error.config?.url
    });
    return Promise.reject(error);
  }
);

export default apiClient;
```

### 5.2 Video Call API 서비스

**`src/services/api/videoCallApi.js`:**

```javascript
import apiClient from './client';
import { API_CONFIG } from '../../utils/constants';

export const videoCallApi = {
  // 세션 생성
  createSession: async (sessionName) => {
    try {
      const response = await apiClient.post(API_CONFIG.ENDPOINTS.SESSIONS, {
        sessionName
      });
      return response.data;
    } catch (error) {
      throw new Error(`세션 생성 실패: ${error.response?.data?.message || error.message}`);
    }
  },

  // 세션 참가
  joinSession: async (sessionId, username) => {
    try {
      const response = await apiClient.post(
        API_CONFIG.ENDPOINTS.JOIN_SESSION(sessionId),
        { username }
      );
      return response.data;
    } catch (error) {
      throw new Error(`세션 참가 실패: ${error.response?.data?.message || error.message}`);
    }
  },

  // 세션 나가기
  leaveSession: async (sessionId, connectionId) => {
    try {
      const response = await apiClient.delete(
        API_CONFIG.ENDPOINTS.LEAVE_SESSION(sessionId, connectionId)
      );
      return response.data;
    } catch (error) {
      throw new Error(`세션 나가기 실패: ${error.response?.data?.message || error.message}`);
    }
  },

  // 세션 종료
  endSession: async (sessionId) => {
    try {
      const response = await apiClient.delete(
        API_CONFIG.ENDPOINTS.END_SESSION(sessionId)
      );
      return response.data;
    } catch (error) {
      throw new Error(`세션 종료 실패: ${error.response?.data?.message || error.message}`);
    }
  }
};
```

---

## 6. React 컴포넌트 구현

### 6.1 상태 관리 (Zustand)

**`src/store/videoCallStore.js`:**

```javascript
import { create } from 'zustand';
import { subscribeWithSelector } from 'zustand/middleware';

export const useVideoCallStore = create(
  subscribeWithSelector((set, get) => ({
    // 상태
    sessionData: null,
    isConnected: false,
    isLoading: false,
    error: null,
    participants: [],
    localVideo: {
      enabled: true,
      muted: false
    },
    localAudio: {
      enabled: true,
      muted: false
    },
    chatMessages: [],

    // 액션
    setSessionData: (sessionData) => set({ sessionData }),
    setConnected: (isConnected) => set({ isConnected }),
    setLoading: (isLoading) => set({ isLoading }),
    setError: (error) => set({ error }),
    
    addParticipant: (participant) => set((state) => ({
      participants: [...state.participants, participant]
    })),
    
    removeParticipant: (connectionId) => set((state) => ({
      participants: state.participants.filter(p => p.connectionId !== connectionId)
    })),
    
    toggleLocalVideo: () => set((state) => ({
      localVideo: { ...state.localVideo, enabled: !state.localVideo.enabled }
    })),
    
    toggleLocalAudio: () => set((state) => ({
      localAudio: { ...state.localAudio, enabled: !state.localAudio.enabled }
    })),
    
    addChatMessage: (message) => set((state) => ({
      chatMessages: [...state.chatMessages, {
        ...message,
        timestamp: new Date().toISOString(),
        id: Date.now() + Math.random()
      }]
    })),

    // 초기화
    reset: () => set({
      sessionData: null,
      isConnected: false,
      isLoading: false,
      error: null,
      participants: [],
      localVideo: { enabled: true, muted: false },
      localAudio: { enabled: true, muted: false },
      chatMessages: []
    })
  }))
);
```

### 6.2 OpenVidu 커스텀 훅

**`src/hooks/useOpenVidu.js`:**

```javascript
import { useState, useEffect, useRef, useCallback } from 'react';
import { OpenVidu } from 'openvidu-browser';
import { useVideoCallStore } from '../store/videoCallStore';
import { OPENVIDU_CONFIG } from '../utils/constants';

export const useOpenVidu = () => {
  const [openVidu, setOpenVidu] = useState(null);
  const [session, setSession] = useState(null);
  const [publisher, setPublisher] = useState(null);
  const [subscribers, setSubscribers] = useState([]);
  
  const sessionRef = useRef(null);
  const publisherRef = useRef(null);
  
  const {
    setConnected,
    setLoading,
    setError,
    addParticipant,
    removeParticipant,
    addChatMessage,
    localVideo,
    localAudio
  } = useVideoCallStore();

  // OpenVidu 초기화
  useEffect(() => {
    const ov = new OpenVidu();
    setOpenVidu(ov);
    
    return () => {
      cleanup();
    };
  }, []);

  // WebSocket URL 리디렉션 설정
  useEffect(() => {
    if (typeof window !== 'undefined' && window.WebSocket) {
      const originalWebSocket = window.WebSocket;
      window.WebSocket = function(url, protocols) {
        if (url.includes('localhost:4443')) {
          url = url.replace('localhost:4443', 'localhost:25565');
          console.log('WebSocket URL redirected to:', url);
        }
        return new originalWebSocket(url, protocols);
      };
    }
  }, []);

  // 세션 연결
  const connectToSession = useCallback(async (sessionData) => {
    if (!openVidu || !sessionData) return;

    try {
      setLoading(true);
      setError(null);

      const newSession = openVidu.initSession();
      sessionRef.current = newSession;
      setSession(newSession);

      // 세션 이벤트 설정
      setupSessionEvents(newSession);

      // 세션 연결
      await newSession.connect(sessionData.token, {
        username: sessionData.username
      });

      // 퍼블리셔 생성
      const newPublisher = await createPublisher();
      
      // 퍼블리셔 발행
      await newSession.publish(newPublisher);
      
      setConnected(true);
      addChatMessage({
        type: 'system',
        username: '시스템',
        message: '화상통화에 연결되었습니다.'
      });

    } catch (error) {
      console.error('세션 연결 오류:', error);
      setError(`연결 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  }, [openVidu, setLoading, setError, setConnected, addChatMessage]);

  // 퍼블리셔 생성
  const createPublisher = useCallback(async () => {
    if (!openVidu) return null;

    try {
      const publisher = await openVidu.initPublisher(undefined, {
        audioSource: undefined,
        videoSource: undefined,
        publishAudio: localAudio.enabled,
        publishVideo: localVideo.enabled,
        resolution: '640x480',
        frameRate: 30,
        insertMode: 'APPEND',
        mirror: false
      });

      publisherRef.current = publisher;
      setPublisher(publisher);
      
      return publisher;
    } catch (error) {
      console.error('퍼블리셔 생성 오류:', error);
      throw error;
    }
  }, [openVidu, localAudio.enabled, localVideo.enabled]);

  // 세션 이벤트 설정
  const setupSessionEvents = useCallback((session) => {
    // 새로운 스트림 생성
    session.on('streamCreated', (event) => {
      const subscriber = session.subscribe(event.stream, undefined);
      setSubscribers(prev => [...prev, subscriber]);
      
      const username = getStreamUsername(event.stream);
      addParticipant({
        connectionId: event.stream.connection.connectionId,
        username,
        subscriber
      });
      
      addChatMessage({
        type: 'system',
        username: '시스템',
        message: `${username}님이 입장했습니다.`
      });
    });

    // 스트림 제거
    session.on('streamDestroyed', (event) => {
      const username = getStreamUsername(event.stream);
      removeParticipant(event.stream.connection.connectionId);
      
      setSubscribers(prev => 
        prev.filter(sub => sub.stream.streamId !== event.stream.streamId)
      );
      
      addChatMessage({
        type: 'system',
        username: '시스템',
        message: `${username}님이 나갔습니다.`
      });
    });

    // 채팅 메시지
    session.on('signal:chat', (event) => {
      const data = JSON.parse(event.data);
      addChatMessage({
        type: 'chat',
        username: data.username,
        message: data.message,
        isOwn: false
      });
    });

    // 연결 오류
    session.on('exception', (exception) => {
      console.error('세션 예외:', exception);
      setError(`세션 오류: ${exception.message}`);
    });
  }, [addParticipant, removeParticipant, addChatMessage, setError]);

  // 스트림에서 사용자명 추출
  const getStreamUsername = useCallback((stream) => {
    try {
      const connectionData = JSON.parse(stream.connection.data);
      return connectionData.username || '사용자';
    } catch (e) {
      return '사용자';
    }
  }, []);

  // 비디오/오디오 토글
  const toggleVideo = useCallback(() => {
    if (publisher) {
      publisher.publishVideo(!localVideo.enabled);
    }
  }, [publisher, localVideo.enabled]);

  const toggleAudio = useCallback(() => {
    if (publisher) {
      publisher.publishAudio(!localAudio.enabled);
    }
  }, [publisher, localAudio.enabled]);

  // 채팅 메시지 전송
  const sendChatMessage = useCallback(async (message) => {
    if (!session || !message.trim()) return;

    try {
      const chatData = {
        username: useVideoCallStore.getState().sessionData?.username,
        message: message.trim(),
        timestamp: new Date().toISOString()
      };

      await session.signal({
        data: JSON.stringify(chatData),
        type: 'chat'
      });

      addChatMessage({
        type: 'chat',
        username: chatData.username,
        message: chatData.message,
        isOwn: true
      });
    } catch (error) {
      console.error('메시지 전송 오류:', error);
      setError('메시지 전송에 실패했습니다.');
    }
  }, [session, addChatMessage, setError]);

  // 세션 나가기
  const leaveSession = useCallback(async () => {
    try {
      if (sessionRef.current) {
        await sessionRef.current.disconnect();
      }
      cleanup();
    } catch (error) {
      console.error('세션 나가기 오류:', error);
    }
  }, []);

  // 정리
  const cleanup = useCallback(() => {
    setSession(null);
    setPublisher(null);
    setSubscribers([]);
    setConnected(false);
    sessionRef.current = null;
    publisherRef.current = null;
  }, [setConnected]);

  return {
    openVidu,
    session,
    publisher,
    subscribers,
    connectToSession,
    leaveSession,
    toggleVideo,
    toggleAudio,
    sendChatMessage
  };
};
```

### 6.3 메인 페이지 컴포넌트

**`src/pages/Home/Home.jsx`:**

```jsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useVideoCallStore } from '../../store/videoCallStore';
import { videoCallApi } from '../../services/api/videoCallApi';
import Button from '../../components/common/Button/Button';
import Input from '../../components/common/Input/Input';
import Loading from '../../components/common/Loading/Loading';
import './Home.scss';

const Home = () => {
  const navigate = useNavigate();
  const { setSessionData, isLoading, setLoading, error, setError } = useVideoCallStore();
  
  const [sessionName, setSessionName] = useState('');
  const [joinSessionId, setJoinSessionId] = useState('');
  const [username, setUsername] = useState('');

  // 세션 생성
  const handleCreateSession = async () => {
    if (!sessionName.trim()) {
      setError('세션 이름을 입력해주세요.');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      // 세션 생성
      const sessionData = await videoCallApi.createSession(sessionName.trim());
      
      // 자동으로 세션에 참가
      const autoUsername = `사용자${Math.floor(Math.random() * 1000)}`;
      const joinData = await videoCallApi.joinSession(sessionData.sessionId, autoUsername);
      
      // 세션 데이터 저장
      setSessionData({
        sessionId: joinData.sessionId,
        sessionName: sessionData.sessionName,
        username: joinData.username,
        token: joinData.token
      });

      // 화상통화 페이지로 이동
      navigate('/video-call');
      
    } catch (error) {
      console.error('세션 생성 오류:', error);
      setError(error.message);
    } finally {
      setLoading(false);
    }
  };

  // 세션 참가
  const handleJoinSession = async () => {
    if (!joinSessionId.trim() || !username.trim()) {
      setError('세션 ID와 사용자명을 모두 입력해주세요.');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const joinData = await videoCallApi.joinSession(
        joinSessionId.trim(),
        username.trim()
      );

      setSessionData({
        sessionId: joinData.sessionId,
        username: joinData.username,
        token: joinData.token
      });

      navigate('/video-call');
      
    } catch (error) {
      console.error('세션 참가 오류:', error);
      setError(error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e, action) => {
    if (e.key === 'Enter') {
      action();
    }
  };

  return (
    <div className="home">
      <div className="home__container">
        <header className="home__header">
          <h1 className="home__title">OpenVidu 화상통화</h1>
          <p className="home__subtitle">실시간 화상통화 서비스</p>
        </header>

        <main className="home__main">
          {/* 세션 생성 섹션 */}
          <section className="home__section">
            <h2 className="home__section-title">새 세션 생성</h2>
            <div className="home__form">
              <Input
                type="text"
                placeholder="세션 이름을 입력하세요"
                value={sessionName}
                onChange={(e) => setSessionName(e.target.value)}
                onKeyPress={(e) => handleKeyPress(e, handleCreateSession)}
                disabled={isLoading}
              />
              <Button
                onClick={handleCreateSession}
                disabled={isLoading || !sessionName.trim()}
                variant="primary"
              >
                세션 생성 및 참가
              </Button>
            </div>
          </section>

          {/* 구분선 */}
          <div className="home__divider">
            <span>또는</span>
          </div>

          {/* 세션 참가 섹션 */}
          <section className="home__section">
            <h2 className="home__section-title">기존 세션 참가</h2>
            <div className="home__form">
              <Input
                type="text"
                placeholder="세션 ID를 입력하세요"
                value={joinSessionId}
                onChange={(e) => setJoinSessionId(e.target.value)}
                disabled={isLoading}
              />
              <Input
                type="text"
                placeholder="사용자명을 입력하세요"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                onKeyPress={(e) => handleKeyPress(e, handleJoinSession)}
                disabled={isLoading}
              />
              <Button
                onClick={handleJoinSession}
                disabled={isLoading || !joinSessionId.trim() || !username.trim()}
                variant="secondary"
              >
                세션 참가
              </Button>
            </div>
          </section>

          {/* 에러 메시지 */}
          {error && (
            <div className="home__error">
              <p>{error}</p>
              <Button
                onClick={() => setError(null)}
                variant="ghost"
                size="small"
              >
                닫기
              </Button>
            </div>
          )}
        </main>

        {/* 로딩 오버레이 */}
        {isLoading && (
          <div className="home__loading">
            <Loading message="처리 중..." />
          </div>
        )}
      </div>
    </div>
  );
};

export default Home;
```

### 6.4 화상통화 페이지 컴포넌트

**`src/pages/VideoCall/VideoCall.jsx`:**

```jsx
import React, { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useVideoCallStore } from '../../store/videoCallStore';
import { useOpenVidu } from '../../hooks/useOpenVidu';
import VideoControls from '../../components/video/VideoControls/VideoControls';
import VideoGrid from '../../components/video/VideoGrid/VideoGrid';
import LocalVideo from '../../components/video/LocalVideo/LocalVideo';
import Chat from '../../components/video/Chat/Chat';
import Loading from '../../components/common/Loading/Loading';
import './VideoCall.scss';

const VideoCall = () => {
  const navigate = useNavigate();
  const initialized = useRef(false);
  
  const {
    sessionData,
    isConnected,
    isLoading,
    error,
    reset
  } = useVideoCallStore();
  
  const {
    publisher,
    subscribers,
    connectToSession,
    leaveSession,
    toggleVideo,
    toggleAudio,
    sendChatMessage
  } = useOpenVidu();

  // 세션 데이터 확인 및 연결
  useEffect(() => {
    if (!sessionData) {
      alert('세션 정보가 없습니다. 메인 페이지로 이동합니다.');
      navigate('/');
      return;
    }

    if (!initialized.current && !isConnected) {
      initialized.current = true;
      connectToSession(sessionData);
    }
  }, [sessionData, isConnected, connectToSession, navigate]);

  // 페이지 언로드 시 정리
  useEffect(() => {
    const handleBeforeUnload = () => {
      leaveSession();
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
      if (isConnected) {
        leaveSession();
      }
    };
  }, [leaveSession, isConnected]);

  // 세션 나가기
  const handleLeaveSession = async () => {
    try {
      await leaveSession();
      reset();
      navigate('/');
    } catch (error) {
      console.error('세션 나가기 오류:', error);
      reset();
      navigate('/');
    }
  };

  if (!sessionData) {
    return null;
  }

  return (
    <div className="video-call">
      <header className="video-call__header">
        <div className="video-call__session-info">
          <h1 className="video-call__title">화상통화</h1>
          <div className="video-call__details">
            <span className="video-call__session-id">
              세션: {sessionData.sessionId}
            </span>
            <span className="video-call__username">
              사용자: {sessionData.username}
            </span>
          </div>
        </div>
        
        <VideoControls
          onToggleVideo={toggleVideo}
          onToggleAudio={toggleAudio}
          onLeaveSession={handleLeaveSession}
          disabled={!isConnected}
        />
      </header>

      <main className="video-call__main">
        <div className="video-call__video-section">
          {/* 로컬 비디오 */}
          <div className="video-call__local-video">
            <LocalVideo publisher={publisher} username={sessionData.username} />
          </div>

          {/* 원격 비디오들 */}
          <div className="video-call__remote-videos">
            <VideoGrid subscribers={subscribers} />
          </div>
        </div>

        {/* 채팅 */}
        <div className="video-call__chat">
          <Chat onSendMessage={sendChatMessage} />
        </div>
      </main>

      {/* 에러 메시지 */}
      {error && (
        <div className="video-call__error">
          <div className="video-call__error-content">
            <p>{error}</p>
            <button onClick={() => useVideoCallStore.getState().setError(null)}>
              닫기
            </button>
          </div>
        </div>
      )}

      {/* 로딩 오버레이 */}
      {isLoading && (
        <div className="video-call__loading">
          <Loading message="연결 중..." />
        </div>
      )}
    </div>
  );
};

export default VideoCall;
```

### 6.5 비디오 컨트롤 컴포넌트

**`src/components/video/VideoControls/VideoControls.jsx`:**

```jsx
import React from 'react';
import { useVideoCallStore } from '../../../store/videoCallStore';
import Button from '../../common/Button/Button';
import { FaVideo, FaVideoSlash, FaMicrophone, FaMicrophoneSlash, FaSignOutAlt } from 'react-icons/fa';
import './VideoControls.scss';

const VideoControls = ({ onToggleVideo, onToggleAudio, onLeaveSession, disabled }) => {
  const { localVideo, localAudio, toggleLocalVideo, toggleLocalAudio } = useVideoCallStore();

  const handleVideoToggle = () => {
    toggleLocalVideo();
    onToggleVideo();
  };

  const handleAudioToggle = () => {
    toggleLocalAudio();
    onToggleAudio();
  };

  return (
    <div className="video-controls">
      <Button
        onClick={handleVideoToggle}
        disabled={disabled}
        className={`video-controls__button ${!localVideo.enabled ? 'video-controls__button--disabled' : ''}`}
        variant="control"
        title={localVideo.enabled ? '비디오 끄기' : '비디오 켜기'}
      >
        {localVideo.enabled ? <FaVideo /> : <FaVideoSlash />}
      </Button>

      <Button
        onClick={handleAudioToggle}
        disabled={disabled}
        className={`video-controls__button ${!localAudio.enabled ? 'video-controls__button--disabled' : ''}`}
        variant="control"
        title={localAudio.enabled ? '오디오 끄기' : '오디오 켜기'}
      >
        {localAudio.enabled ? <FaMicrophone /> : <FaMicrophoneSlash />}
      </Button>

      <Button
        onClick={onLeaveSession}
        disabled={disabled}
        className="video-controls__button video-controls__button--leave"
        variant="danger"
        title="나가기"
      >
        <FaSignOutAlt />
        <span>나가기</span>
      </Button>
    </div>
  );
};

export default VideoControls;
```

### 6.6 로컬 비디오 컴포넌트

**`src/components/video/LocalVideo/LocalVideo.jsx`:**

```jsx
import React, { useEffect, useRef } from 'react';
import { useVideoCallStore } from '../../../store/videoCallStore';
import './LocalVideo.scss';

const LocalVideo = ({ publisher, username }) => {
  const videoRef = useRef(null);
  const { localVideo } = useVideoCallStore();

  useEffect(() => {
    if (publisher && videoRef.current) {
      publisher.addVideoElement(videoRef.current);
    }

    return () => {
      if (publisher && videoRef.current) {
        // 정리 작업
      }
    };
  }, [publisher]);

  return (
    <div className="local-video">
      <div className="local-video__container">
        <video
          ref={videoRef}
          autoPlay
          playsInline
          muted
          className={`local-video__element ${!localVideo.enabled ? 'local-video__element--disabled' : ''}`}
        />
        
        <div className="local-video__overlay">
          <span className="local-video__label">{username} (나)</span>
          {!localVideo.enabled && (
            <div className="local-video__disabled-indicator">
              비디오 꺼짐
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default LocalVideo;
```

### 6.7 비디오 그리드 컴포넌트

**`src/components/video/VideoGrid/VideoGrid.jsx`:**

```jsx
import React from 'react';
import RemoteVideo from '../RemoteVideo/RemoteVideo';
import './VideoGrid.scss';

const VideoGrid = ({ subscribers }) => {
  if (!subscribers || subscribers.length === 0) {
    return (
      <div className="video-grid video-grid--empty">
        <div className="video-grid__empty-message">
          <p>다른 참가자를 기다리는 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className={`video-grid video-grid--count-${subscribers.length}`}>
      {subscribers.map((subscriber) => (
        <RemoteVideo
          key={subscriber.stream.streamId}
          subscriber={subscriber}
        />
      ))}
    </div>
  );
};

export default VideoGrid;
```

### 6.8 원격 비디오 컴포넌트

**`src/components/video/RemoteVideo/RemoteVideo.jsx`:**

```jsx
import React, { useEffect, useRef } from 'react';
import './RemoteVideo.scss';

const RemoteVideo = ({ subscriber }) => {
  const videoRef = useRef(null);

  useEffect(() => {
    if (subscriber && videoRef.current) {
      subscriber.addVideoElement(videoRef.current);
    }

    return () => {
      if (subscriber && videoRef.current) {
        // 정리 작업
      }
    };
  }, [subscriber]);

  const getUsername = () => {
    try {
      const connectionData = JSON.parse(subscriber.stream.connection.data);
      return connectionData.username || '사용자';
    } catch (e) {
      return '사용자';
    }
  };

  return (
    <div className="remote-video">
      <div className="remote-video__container">
        <video
          ref={videoRef}
          autoPlay
          playsInline
          className="remote-video__element"
        />
        
        <div className="remote-video__overlay">
          <span className="remote-video__label">{getUsername()}</span>
        </div>
      </div>
    </div>
  );
};

export default RemoteVideo;
```

### 6.9 채팅 컴포넌트

**`src/components/video/Chat/Chat.jsx`:**

```jsx
import React, { useState, useRef, useEffect } from 'react';
import { useVideoCallStore } from '../../../store/videoCallStore';
import Button from '../../common/Button/Button';
import { FaPaperPlane } from 'react-icons/fa';
import { CHAT_CONFIG } from '../../../utils/constants';
import './Chat.scss';

const Chat = ({ onSendMessage }) => {
  const [message, setMessage] = useState('');
  const [isExpanded, setIsExpanded] = useState(false);
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);
  
  const { chatMessages } = useVideoCallStore();

  // 메시지 목록 자동 스크롤
  useEffect(() => {
    scrollToBottom();
  }, [chatMessages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const handleSendMessage = () => {
    const trimmedMessage = message.trim();
    if (!trimmedMessage) return;

    onSendMessage(trimmedMessage);
    setMessage('');
    inputRef.current?.focus();
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const formatTime = (timestamp) => {
    return new Date(timestamp).toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className={`chat ${isExpanded ? 'chat--expanded' : ''}`}>
      <div className="chat__header">
        <h3 className="chat__title">채팅</h3>
        <Button
          onClick={() => setIsExpanded(!isExpanded)}
          variant="ghost"
          size="small"
          className="chat__toggle"
        >
          {isExpanded ? '접기' : '펼치기'}
        </Button>
      </div>

      <div className="chat__messages" id="chatMessages">
        {chatMessages.length === 0 ? (
          <div className="chat__empty">
            <p>아직 메시지가 없습니다.</p>
          </div>
        ) : (
          chatMessages.map((msg) => (
            <div
              key={msg.id}
              className={`chat__message ${msg.isOwn ? 'chat__message--own' : ''} ${
                msg.type === 'system' ? 'chat__message--system' : ''
              }`}
            >
              <div className="chat__message-header">
                <span className="chat__username">{msg.username}</span>
                <span className="chat__time">{formatTime(msg.timestamp)}</span>
              </div>
              <div className="chat__message-content">{msg.message}</div>
            </div>
          ))
        )}
        <div ref={messagesEndRef} />
      </div>

      <div className="chat__input">
        <div className="chat__input-container">
          <textarea
            ref={inputRef}
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="메시지를 입력하세요..."
            maxLength={CHAT_CONFIG.MAX_MESSAGE_LENGTH}
            rows="1"
            className="chat__textarea"
          />
          <Button
            onClick={handleSendMessage}
            disabled={!message.trim()}
            variant="primary"
            size="small"
            className="chat__send-button"
          >
            <FaPaperPlane />
          </Button>
        </div>
        <div className="chat__input-info">
          <span className="chat__char-count">
            {message.length}/{CHAT_CONFIG.MAX_MESSAGE_LENGTH}
          </span>
        </div>
      </div>
    </div>
  );
};

export default Chat;
```

---

## 7. OpenVidu 통합

### 7.1 OpenVidu 설정 서비스

**`src/services/openvidu/openviduConfig.js`:**

```javascript
import { OPENVIDU_CONFIG } from '../../utils/constants';

export class OpenViduConfigService {
  static configureWebSocketRedirection() {
    if (typeof window !== 'undefined' && window.WebSocket) {
      const originalWebSocket = window.WebSocket;
      
      window.WebSocket = function(url, protocols) {
        // OpenVidu 기본 포트(4443)를 사용자 정의 포트로 리디렉션
        if (url.includes('localhost:4443')) {
          const customPort = OPENVIDU_CONFIG.SERVER_URL.split(':')[2] || '25565';
          url = url.replace('localhost:4443', `localhost:${customPort}`);
          console.log('WebSocket URL redirected to:', url);
        }
        
        return new originalWebSocket(url, protocols);
      };
    }
  }

  static validateConfiguration() {
    const errors = [];
    
    if (!OPENVIDU_CONFIG.SERVER_URL) {
      errors.push('OpenVidu 서버 URL이 설정되지 않았습니다.');
    }
    
    if (!OPENVIDU_CONFIG.SERVER_SECRET) {
      errors.push('OpenVidu 서버 시크릿이 설정되지 않았습니다.');
    }
    
    return {
      isValid: errors.length === 0,
      errors
    };
  }

  static getServerInfo() {
    return {
      url: OPENVIDU_CONFIG.SERVER_URL,
      isDevelopment: process.env.NODE_ENV === 'development',
      isLocalhost: OPENVIDU_CONFIG.SERVER_URL.includes('localhost')
    };
  }
}
```

---

## 8. 스타일링

### 8.1 전역 스타일

**`src/styles/globals.scss`:**

```scss
// 변수 import
@import './variables.scss';

// 초기화
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #root {
  height: 100%;
  font-family: $font-family-base;
  background-color: $bg-color;
  color: $text-color;
}

// 공통 클래스
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 $spacing-md;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

// 버튼 초기화
button {
  background: none;
  border: none;
  cursor: pointer;
  font-family: inherit;
  
  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

// 입력 초기화
input, textarea {
  font-family: inherit;
  border: none;
  outline: none;
  
  &:focus {
    outline: 2px solid $primary-color;
    outline-offset: 2px;
  }
}

// 비디오 요소
video {
  display: block;
  max-width: 100%;
  height: auto;
}
```

### 8.2 변수 설정

**`src/styles/variables.scss`:**

```scss
// 색상
$primary-color: #007bff;
$secondary-color: #6c757d;
$success-color: #28a745;
$danger-color: #dc3545;
$warning-color: #ffc107;
$info-color: #17a2b8;

$bg-color: #f8f9fa;
$surface-color: #ffffff;
$text-color: #212529;
$text-muted: #6c757d;
$border-color: #dee2e6;

// 스페이싱
$spacing-xs: 0.25rem;
$spacing-sm: 0.5rem;
$spacing-md: 1rem;
$spacing-lg: 1.5rem;
$spacing-xl: 2rem;
$spacing-xxl: 3rem;

// 폰트
$font-family-base: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
$font-size-sm: 0.875rem;
$font-size-base: 1rem;
$font-size-lg: 1.125rem;
$font-size-xl: 1.25rem;
$font-size-xxl: 1.5rem;

// 반응형 브레이크포인트
$breakpoint-sm: 576px;
$breakpoint-md: 768px;
$breakpoint-lg: 992px;
$breakpoint-xl: 1200px;

// 그림자
$shadow-sm: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.075);
$shadow-md: 0 0.5rem 1rem rgba(0, 0, 0, 0.15);
$shadow-lg: 0 1rem 3rem rgba(0, 0, 0, 0.175);

// 테두리 반경
$border-radius-sm: 0.25rem;
$border-radius-md: 0.375rem;
$border-radius-lg: 0.5rem;

// 애니메이션
$transition-base: all 0.2s ease-in-out;
$transition-fade: opacity 0.15s linear;
$transition-collapse: height 0.35s ease;
```

### 8.3 Home 페이지 스타일

**`src/pages/Home/Home.scss`:**

```scss
@import '../../styles/variables.scss';

.home {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: $spacing-md;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  &__container {
    background: $surface-color;
    border-radius: $border-radius-lg;
    box-shadow: $shadow-lg;
    padding: $spacing-xxl;
    width: 100%;
    max-width: 500px;
    position: relative;
  }

  &__header {
    text-align: center;
    margin-bottom: $spacing-xl;
  }

  &__title {
    color: $primary-color;
    font-size: $font-size-xxl;
    font-weight: 700;
    margin-bottom: $spacing-sm;
  }

  &__subtitle {
    color: $text-muted;
    font-size: $font-size-base;
  }

  &__section {
    margin-bottom: $spacing-xl;

    &:last-child {
      margin-bottom: 0;
    }
  }

  &__section-title {
    font-size: $font-size-lg;
    font-weight: 600;
    margin-bottom: $spacing-md;
    color: $text-color;
  }

  &__form {
    display: flex;
    flex-direction: column;
    gap: $spacing-md;
  }

  &__divider {
    display: flex;
    align-items: center;
    margin: $spacing-xl 0;
    
    &::before,
    &::after {
      content: '';
      flex: 1;
      height: 1px;
      background: $border-color;
    }

    span {
      padding: 0 $spacing-md;
      color: $text-muted;
      font-size: $font-size-sm;
    }
  }

  &__error {
    background: rgba($danger-color, 0.1);
    border: 1px solid rgba($danger-color, 0.3);
    border-radius: $border-radius-md;
    padding: $spacing-md;
    margin-top: $spacing-md;
    display: flex;
    justify-content: space-between;
    align-items: center;

    p {
      color: $danger-color;
      margin: 0;
      font-size: $font-size-sm;
    }
  }

  &__loading {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba($surface-color, 0.9);
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: $border-radius-lg;
  }

  @media (max-width: $breakpoint-sm) {
    padding: $spacing-sm;

    &__container {
      padding: $spacing-xl;
    }

    &__title {
      font-size: $font-size-xl;
    }
  }
}
```

### 8.4 VideoCall 페이지 스타일

**`src/pages/VideoCall/VideoCall.scss`:**

```scss
@import '../../styles/variables.scss';

.video-call {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #1a1a1a;
  color: white;

  &__header {
    background: rgba(0, 0, 0, 0.8);
    padding: $spacing-md;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  &__session-info {
    display: flex;
    flex-direction: column;
    gap: $spacing-xs;
  }

  &__title {
    font-size: $font-size-lg;
    font-weight: 600;
    margin: 0;
  }

  &__details {
    display: flex;
    gap: $spacing-md;
    font-size: $font-size-sm;
    color: rgba(255, 255, 255, 0.7);
  }

  &__main {
    flex: 1;
    display: grid;
    grid-template-columns: 1fr 300px;
    gap: $spacing-md;
    padding: $spacing-md;
    overflow: hidden;

    @media (max-width: $breakpoint-lg) {
      grid-template-columns: 1fr;
      grid-template-rows: 1fr auto;
    }
  }

  &__video-section {
    display: flex;
    flex-direction: column;
    gap: $spacing-md;
    min-height: 0;
  }

  &__local-video {
    flex-shrink: 0;
  }

  &__remote-videos {
    flex: 1;
    min-height: 0;
  }

  &__chat {
    background: rgba(0, 0, 0, 0.5);
    border-radius: $border-radius-md;
    overflow: hidden;

    @media (max-width: $breakpoint-lg) {
      max-height: 300px;
    }
  }

  &__error {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.8);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
  }

  &__error-content {
    background: $surface-color;
    color: $text-color;
    padding: $spacing-xl;
    border-radius: $border-radius-md;
    box-shadow: $shadow-lg;
    max-width: 400px;
    text-align: center;

    p {
      margin-bottom: $spacing-md;
    }

    button {
      background: $primary-color;
      color: white;
      padding: $spacing-sm $spacing-md;
      border-radius: $border-radius-sm;
      transition: $transition-base;

      &:hover {
        background: darken($primary-color, 10%);
      }
    }
  }

  &__loading {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.8);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
  }
}
```

---

## 9. 테스트 및 디버깅

### 9.1 개발 환경 설정

**`package.json` scripts 수정:**

```json
{
  "scripts": {
    "start": "react-scripts start",
    "build": "react-scripts build",
    "test": "react-scripts test",
    "eject": "react-scripts eject",
    "dev": "REACT_APP_DEBUG_MODE=true npm start",
    "build:dev": "REACT_APP_ENVIRONMENT=development npm run build"
  }
}
```

### 9.2 디버깅 유틸리티

**`src/utils/debug.js`:**

```javascript
export const DEBUG_MODE = process.env.REACT_APP_DEBUG_MODE === 'true';

export const debugLog = (category, message, data = null) => {
  if (!DEBUG_MODE) return;
  
  const timestamp = new Date().toISOString();
  const prefix = `[${timestamp}] [${category}]`;
  
  if (data) {
    console.group(prefix, message);
    console.log(data);
    console.groupEnd();
  } else {
    console.log(prefix, message);
  }
};

export const debugError = (category, error, context = null) => {
  console.group(`[ERROR] [${category}]`);
  console.error(error);
  if (context) {
    console.log('Context:', context);
  }
  console.groupEnd();
};

export const debugTimer = (label) => {
  if (!DEBUG_MODE) return { end: () => {} };
  
  console.time(label);
  
  return {
    end: () => console.timeEnd(label)
  };
};
```

### 9.3 에러 바운더리

**`src/components/common/ErrorBoundary/ErrorBoundary.jsx`:**

```jsx
import React from 'react';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    this.setState({
      error,
      errorInfo
    });
    
    console.error('ErrorBoundary caught an error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="error-boundary">
          <div className="error-boundary__container">
            <h2>문제가 발생했습니다</h2>
            <p>예상치 못한 오류가 발생했습니다. 페이지를 새로고침해 주세요.</p>
            
            {process.env.NODE_ENV === 'development' && (
              <details style={{ whiteSpace: 'pre-wrap', marginTop: '1rem' }}>
                <summary>오류 세부사항</summary>
                {this.state.error && this.state.error.toString()}
                <br />
                {this.state.errorInfo.componentStack}
              </details>
            )}
            
            <button
              onClick={() => window.location.reload()}
              style={{
                marginTop: '1rem',
                padding: '0.5rem 1rem',
                backgroundColor: '#007bff',
                color: 'white',
                border: 'none',
                borderRadius: '0.25rem',
                cursor: 'pointer'
              }}
            >
              페이지 새로고침
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
```

---

## 10. 배포 준비

### 10.1 환경별 설정

**프로덕션 환경 변수 (`.env.production`):**

```env
REACT_APP_OPENVIDU_SERVER_URL=https://your-openvidu-server.com
REACT_APP_OPENVIDU_SERVER_SECRET=your-production-secret
REACT_APP_API_BASE_URL=https://your-api-server.com
REACT_APP_ENVIRONMENT=production
REACT_APP_DEBUG_MODE=false
```

### 10.2 빌드 최적화

**`src/index.js` 수정:**

```jsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import ErrorBoundary from './components/common/ErrorBoundary/ErrorBoundary';
import { OpenViduConfigService } from './services/openvidu/openviduConfig';
import './styles/globals.scss';

// OpenVidu 설정 검증
const configValidation = OpenViduConfigService.validateConfiguration();
if (!configValidation.isValid) {
  console.error('OpenVidu 설정 오류:', configValidation.errors);
}

// WebSocket 리디렉션 설정
OpenViduConfigService.configureWebSocketRedirection();

const root = ReactDOM.createRoot(document.getElementById('root'));

root.render(
  <React.StrictMode>
    <ErrorBoundary>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </ErrorBoundary>
  </React.StrictMode>
);
```

### 10.3 App.js 라우팅 설정

**`src/App.js`:**

```jsx
import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Home from './pages/Home/Home';
import VideoCall from './pages/VideoCall/VideoCall';

function App() {
  return (
    <div className="App">
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/video-call" element={<VideoCall />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  );
}

export default App;
```

---

## 구현 완료 체크리스트

### ✅ 필수 구현 사항
- [ ] React 프로젝트 설정
- [ ] OpenVidu Browser SDK 설치 및 설정
- [ ] 상태 관리 (Zustand) 구현
- [ ] API 서비스 레이어 구현
- [ ] 메인 페이지 (세션 생성/참가) 구현
- [ ] 화상통화 페이지 구현
- [ ] 로컬/원격 비디오 컴포넌트 구현
- [ ] 비디오/오디오 컨트롤 구현
- [ ] 채팅 기능 구현
- [ ] WebSocket URL 리디렉션 처리
- [ ] 에러 처리 및 로딩 상태 관리
- [ ] 반응형 스타일링

### ✅ 추가 구현 사항
- [ ] TypeScript 적용 (선택)
- [ ] 테스트 코드 작성 (선택)
- [ ] PWA 기능 추가 (선택)
- [ ] 화면 공유 기능 (선택)
- [ ] 녹화 기능 (선택)

---

## 주의사항 및 팁

### 🔴 중요한 주의사항
1. **포트 설정**: OpenVidu 서버와 클라이언트 포트 일치 확인
2. **CORS 설정**: Backend API CORS 설정 필요
3. **HTTPS**: 프로덕션에서는 HTTPS 필수 (WebRTC 요구사항)
4. **메모리 관리**: 컴포넌트 언마운트 시 OpenVidu 리소스 정리

### 💡 개발 팁
1. **개발 도구**: React DevTools, Redux DevTools 활용
2. **디버깅**: 브라우저 개발자 도구의 Network, Console 탭 적극 활용
3. **성능**: React.memo, useMemo, useCallback 적절히 사용
4. **코드 분할**: React.lazy와 Suspense를 이용한 코드 스플리팅

이 가이드를 따라 구현하면 현재 Vanilla JavaScript로 구현된 OpenVidu 화상통화 시스템을 React로 성공적으로 마이그레이션할 수 있습니다.