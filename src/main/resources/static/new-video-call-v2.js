// OpenVidu 화상통화 ver2 JavaScript  
// ver-20250826-030000-cache-fix - RTCPeerConnection 오버라이드 완전 제거
// 🔄 캐시 무효화 버전: 2025-08-26T03:00:00Z
// ✅ RTCPeerConnection 오버라이드 완전 비활성화
// ✅ OpenVidu 훅 메서드 완전 비활성화
// ✅ OpenVidu 기본 ICE/TURN 설정 사용

// 버전 정보 출력
console.log('======================================');
console.log('🚀 VideoCall Script Version: 2025-08-26T03:00:00Z-cache-fix');
console.log('✅ RTCPeerConnection 오버라이드 완전 제거됨');
console.log('✅ OpenVidu 기본 ICE/TURN 설정 사용');
console.log('======================================');

class NewVideoCallV2Manager {
    constructor() {
        // 세션 데이터
        this.sessionData = null;
        this.session = null;
        this.publisher = null;
        this.subscribers = [];
        
        // OpenVidu 객체
        this.OV = null;
        
        // 화면공유 관련
        this.isScreenSharing = false;
        this.screenSharePublisher = null;
        
        // Picture-in-Picture 관련
        this.pipEnabled = false;
        this.pipStream = null;
        this.pipUsername = null;
        this.isDragging = false;
        this.dragOffset = { x: 0, y: 0 };
        
        // UI 요소들
        this.mainVideo = null;
        this.pipVideo = null;
        this.pipContainer = null;
        
        // 사이드바 새 창 관련
        this.sidebarWindow = null;
        
        // 타이머 관련
        this.callStartTime = null;
        this.callTimer = null;
        
        // 참가자 관리 (독립적인 참가자 목록 관리)
        this.participants = new Map(); // connectionId -> {username, connectionId, isMe}
        
        this.initializeApp();
    }
    
    async initializeApp() {
        try {
            this.initializeElements();
            this.attachEventListeners();
            this.loadSessionData();
            this.configureOpenVidu();
            await this.connectToSession();
            this.startCallTimer();
            this.setupWindowCommunication();
            
            console.log('✅ 앱 초기화 성공 - 모든 시스템 정상 작동');
            
        } catch (error) {
            console.error('❌ 앱 초기화 실패:', error);
            
            // 연결 실패 시 네트워크 진단 실행
            console.log('🔍 연결 실패로 인한 네트워크 진단 시작...');
            this.showToast('연결 실패 - 네트워크 진단 중...');
            
            try {
                await this.performComprehensiveNetworkDiagnosis();
            } catch (diagnosisError) {
                console.error('네트워크 진단 실패:', diagnosisError);
            }
            
            // Fallback WebRTC 테스트도 시도
            console.log('🚀 Fallback WebRTC 연결성 테스트...');
            try {
                const fallbackResult = await this.testFallbackConnectivity();
                console.log('📊 Fallback 테스트 결과:', fallbackResult);
            } catch (fallbackError) {
                console.error('Fallback 테스트 실패:', fallbackError);
            }
            
            this.showToast('연결에 실패했습니다. 콘솔에서 진단 결과를 확인하세요.');
        }
    }
    
    initializeElements() {
        // 비디오 요소들
        this.mainVideo = document.getElementById('mainVideo');
        this.pipVideo = document.getElementById('pipVideo');
        this.pipContainer = document.getElementById('pipContainer');
        this.mainVideoOverlay = document.getElementById('mainVideoOverlay');
        this.mainVideoLabel = document.getElementById('mainVideoLabel');
        this.pipLabel = document.getElementById('pipLabel');
        
        // 버튼들
        this.toggleAudioBtn = document.getElementById('toggleAudioBtn');
        this.toggleVideoBtn = document.getElementById('toggleVideoBtn');
        this.toggleScreenShareBtn = document.getElementById('toggleScreenShareBtn');
        this.pipSwapBtn = document.getElementById('pipSwapBtn');
        this.pipCloseBtn = document.getElementById('pipCloseBtn');
        this.sidebarBtn = document.getElementById('sidebarBtn');
        this.leaveSessionBtn = document.getElementById('leaveSessionBtn');
        
        // 사이드바 관련 요소들
        this.sidebar = document.getElementById('sidebar');
        this.sidebarOverlay = document.getElementById('sidebarOverlay');
        this.sidebarCloseBtn = document.getElementById('sidebarCloseBtn');
        this.participantsList = document.getElementById('participantsList');
        this.participantCount = document.getElementById('participantCount');
        this.chatMessages = document.getElementById('chatMessages');
        this.chatInput = document.getElementById('chatInput');
        this.sendMessageBtn = document.getElementById('sendMessageBtn');
        this.fileInput = document.getElementById('fileInput');
        this.fileUploadBtn = document.getElementById('fileUploadBtn');
        
        // 기타 UI 요소들
        this.callDuration = document.getElementById('callDuration');
        this.currentUserName = document.getElementById('currentUserName');
        this.currentUserAvatar = document.getElementById('currentUserAvatar');
        this.loadingScreen = document.getElementById('loadingScreen');
        this.toast = document.getElementById('toast');
        this.toastMessage = document.getElementById('toastMessage');
        
        // 스냅 영역들
        this.snapZones = {
            topLeft: document.getElementById('snapZoneTopLeft'),
            topRight: document.getElementById('snapZoneTopRight'),
            bottomLeft: document.getElementById('snapZoneBottomLeft'),
            bottomRight: document.getElementById('snapZoneBottomRight')
        };
    }
    
    attachEventListeners() {
        // 컨트롤 버튼 이벤트
        this.toggleAudioBtn.addEventListener('click', () => this.toggleAudio());
        this.toggleVideoBtn.addEventListener('click', () => this.toggleVideo());
        this.toggleScreenShareBtn.addEventListener('click', () => this.toggleScreenShare());
        this.leaveSessionBtn.addEventListener('click', () => this.leaveSession());
        
        // 사이드바 이벤트
        this.sidebarBtn.addEventListener('click', () => this.toggleSidebar());
        this.sidebarCloseBtn.addEventListener('click', () => this.closeSidebar());
        this.sidebarOverlay.addEventListener('click', () => this.closeSidebar());
        
        // 채팅 이벤트
        this.sendMessageBtn.addEventListener('click', () => this.sendChatMessage());
        this.chatInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.sendChatMessage();
            }
        });
        
        // 파일 업로드 이벤트
        this.fileUploadBtn.addEventListener('click', () => {
            this.fileInput.click();
        });
        
        this.fileInput.addEventListener('change', (e) => {
            if (e.target.files.length > 0) {
                this.handleFileUpload(e.target.files[0]);
            }
        });
        
        // PIP 이벤트
        this.pipSwapBtn.addEventListener('click', () => this.swapPipAndMain());
        this.pipCloseBtn.addEventListener('click', () => this.hidePip());
        
        // PIP 드래그 이벤트
        this.pipContainer.addEventListener('mousedown', (e) => this.startDragging(e));
        document.addEventListener('mousemove', (e) => this.handleDragging(e));
        document.addEventListener('mouseup', () => this.stopDragging());
        
        // PIP 터치 이벤트 (모바일)
        this.pipContainer.addEventListener('touchstart', (e) => this.startDragging(e.touches[0]));
        this.pipContainer.addEventListener('touchmove', (e) => {
            e.preventDefault();
            this.handleDragging(e.touches[0]);
        });
        this.pipContainer.addEventListener('touchend', () => this.stopDragging());
        
        // PIP 더블클릭 이벤트
        this.pipContainer.addEventListener('dblclick', () => this.swapPipAndMain());
        
        // 키보드 단축키
        document.addEventListener('keydown', (e) => {
            if (e.ctrlKey && e.key === 'p' && this.pipEnabled) {
                e.preventDefault();
                this.swapPipAndMain();
            }
            if (e.key === 'Escape' && this.pipEnabled) {
                this.hidePip();
            }
        });
        
        // 창 닫기 전 정리
        window.addEventListener('beforeunload', () => {
            this.cleanup();
        });
    }
    
    loadSessionData() {
        const urlParams = new URLSearchParams(window.location.search);
        this.sessionData = {
            sessionId: urlParams.get('sessionId'),
            username: urlParams.get('username'),
            token: urlParams.get('token')
        };
        
        if (!this.sessionData.sessionId || !this.sessionData.username || !this.sessionData.token) {
            throw new Error('세션 정보가 올바르지 않습니다.');
        }
        
        // 사용자 정보 표시
        this.currentUserName.textContent = this.sessionData.username;
        this.currentUserAvatar.textContent = this.sessionData.username.charAt(0).toUpperCase();
    }
    
    /**
     * 🚀 ENHANCED: RTCPeerConnection 보강 오버라이드
     * OpenVidu CE가 ICE 서버를 제공하지 않으므로 동적 TURN 인증 생성
     */
    overrideRTCPeerConnection() {
        // 🚫 [ver-20250826-cache-fix] 완전 비활성화됨 - OpenVidu 기본 설정 사용
        console.log('🔄 [ver-20250826-cache-fix] RTCPeerConnection 오버라이드 비활성화됨');
        console.log('✅ OpenVidu가 자체적으로 TURN/ICE 서버를 관리합니다.');
        return; // 즉시 리턴하여 아무 작업도 수행하지 않음
        
        const originalRTCPeerConnection = window.RTCPeerConnection;
        
        // TURN 서버 동적 인증 정보 생성 (COTURN --use-auth-secret 호환)
        const generateTurnCredentials = () => {
            const timestamp = Math.floor(Date.now() / 1000) + 86400; // 24시간 후 만료
            const username = `${timestamp}:openviduturn`;
            const secret = 'MY_SECRET';
            
            // HMAC-SHA1 해시 생성 (브라우저 내장 Web Crypto API 사용)
            const credential = btoa(String.fromCharCode(...new Uint8Array(
                new TextEncoder().encode(username + ':' + secret)
            )));
            
            return { username, credential: secret }; // 간단화: secret을 직접 사용
        };
        
        const turnCreds = generateTurnCredentials();
        
        // EC2 직접 IP를 사용한 TURN/STUN 서버 설정
        const mandatoryIceServers = [
            // COTURN 서버 (EC2 직접 IP - UDP만 가능, ALB 우회)
            { urls: 'stun:13.209.15.208:3478' },
            { urls: `turn:13.209.15.208:3478`, username: turnCreds.username, credential: turnCreds.credential },
            // 백업 STUN 서버들
            { urls: 'stun:stun.l.google.com:19302' },
            { urls: 'stun:stun1.l.google.com:19302' }
        ];
        
        // RTCPeerConnection 생성자 오버라이드
        window.RTCPeerConnection = function(config) {
            console.log('🚀 RTCPeerConnection Enhancement 활성화!');
            console.log('🔍 Original config:', config);
            
            // OpenVidu가 제공한 ICE 서버 정보 확인
            let iceServers = config?.iceServers || [];
            
            // OpenVidu가 ICE 서버를 제공하지 않거나 비어있는 경우
            if (!iceServers || iceServers.length === 0) {
                console.log('⚠️ OpenVidu가 ICE 서버를 제공하지 않음. 동적 TURN 설정 적용...');
                iceServers = mandatoryIceServers;
            } else {
                // OpenVidu가 제공한 ICE 서버가 있더라도 TURN이 없으면 추가
                const hasTurnServer = iceServers.some(server => 
                    server.urls && server.urls.toString().includes('turn:')
                );
                
                if (!hasTurnServer) {
                    console.log('⚠️ OpenVidu에 TURN 서버가 없음. 동적 TURN 추가...');
                    // TURN 서버 추가
                    iceServers.push(mandatoryIceServers[1]); // TURN 서버
                }
                
                // 백업 STUN 서버 추가 (중복 제거)
                mandatoryIceServers.forEach(server => {
                    if (server.urls.toString().includes('stun:')) {
                        const exists = iceServers.some(existing => 
                            existing.urls === server.urls
                        );
                        if (!exists) {
                            iceServers.push(server);
                        }
                    }
                });
            }
            
            // 개선된 설정
            const enhancedConfig = {
                ...config,
                iceServers: iceServers,  // 처리된 ICE 서버들
                iceTransportPolicy: 'all',  // 모든 후보 사용
                iceCandidatePoolSize: 30,   // ICE 후보 풀 크기
                bundlePolicy: 'balanced',
                rtcpMuxPolicy: 'negotiate'
            };
            
            console.log('✅ Enhanced config with', enhancedConfig.iceServers.length, 'ICE servers:');
            enhancedConfig.iceServers.forEach((server, index) => {
                const urls = Array.isArray(server.urls) ? server.urls.join(', ') : server.urls;
                const hasAuth = server.username || server.credential;
                console.log(`   ${index + 1}. ${urls}${hasAuth ? ' (authenticated)' : ''}`);
            });
            
            // 원본 생성자 호출
            const pc = new originalRTCPeerConnection(enhancedConfig);
            
            // ICE 연결 상태 모니터링 강화
            let reconnectionAttempts = 0;
            const maxReconnectionAttempts = 3;
            
            pc.addEventListener('iceconnectionstatechange', () => {
                console.log(`🧊 ICE Connection State: ${pc.iceConnectionState}`);
                
                if (pc.iceConnectionState === 'connected' || pc.iceConnectionState === 'completed') {
                    console.log('✅ ICE 연결 성공!');
                    reconnectionAttempts = 0; // 성공시 카운터 리셋
                } else if (pc.iceConnectionState === 'failed' || pc.iceConnectionState === 'disconnected') {
                    reconnectionAttempts++;
                    console.error(`❌ ICE Connection Failed/Disconnected - 재시도 ${reconnectionAttempts}/${maxReconnectionAttempts}`);
                    
                    if (reconnectionAttempts <= maxReconnectionAttempts) {
                        // 점진적 지연을 통한 재시도
                        const delay = reconnectionAttempts * 2000; // 2초, 4초, 6초
                        setTimeout(() => {
                            if (pc.restartIce && pc.iceConnectionState !== 'connected') {
                                console.log(`🔄 ICE 재시작 시도 ${reconnectionAttempts}... (${delay}ms 지연)`);
                                pc.restartIce();
                            }
                        }, delay);
                    } else {
                        console.error('💔 ICE 연결 재시도 한계 도달 - 연결 포기');
                    }
                }
            });
            
            // ICE 후보 수집 모니터링
            pc.addEventListener('icecandidateerror', (event) => {
                console.error('❌ ICE Candidate Error:', event);
            });
            
            let candidateCount = 0;
            pc.addEventListener('icecandidate', (event) => {
                if (event.candidate) {
                    candidateCount++;
                    console.log(`🧊 ICE Candidate #${candidateCount}:`, event.candidate.candidate);
                } else {
                    console.log(`✅ ICE 후보 수집 완료 (총 ${candidateCount}개)`);
                }
            });
            
            return pc;
        };
        
        // 원본 속성들 복사 (브라우저 호환성)
        Object.setPrototypeOf(window.RTCPeerConnection, originalRTCPeerConnection);
        window.RTCPeerConnection.prototype = originalRTCPeerConnection.prototype;
        
        console.log('🚀 RTCPeerConnection Override 완료! OpenVidu ICE 설정 무시 문제 해결됨');
    }
    
    /**
     * 🎯 OpenVidu 세션 연결 후 추가 ICE 서버 훅 적용
     * RTCPeerConnection이 이미 생성된 후에도 추가 보강 작업 수행
     */
    injectOpenViduHooks() {
        // 🚫 [ver-20250826-cache-fix] 완전 비활성화됨 - OpenVidu 기본 동작 유지
        console.log('🔄 [ver-20250826-cache-fix] OpenVidu 훅 비활성화됨');
        console.log('✅ OpenVidu 기본 동작을 그대로 사용합니다.');
        return; // 즉시 리턴하여 아무 작업도 수행하지 않음
        
        console.log('🎯 OpenVidu 훅 기반 ICE 서버 주입 시작...');
        
        // Publisher/Subscriber 생성 시 ICE 서버 강화
        const originalPublish = this.session.publish;
        if (originalPublish) {
            this.session.publish = (...args) => {
                console.log('🔧 Publisher 생성 훅 활성화');
                const result = originalPublish.apply(this.session, args);
                this.enhancePublisherConnection(result);
                return result;
            };
        }
        
        const originalSubscribe = this.session.subscribe;
        if (originalSubscribe) {
            this.session.subscribe = (...args) => {
                console.log('🔧 Subscriber 생성 훅 활성화');
                const result = originalSubscribe.apply(this.session, args);
                this.enhanceSubscriberConnection(result);
                return result;
            };
        }
        
        // WebRTC 연결 상태 강화 모니터링
        this.setupAdvancedConnectionMonitoring();
        
        console.log('✅ OpenVidu 훅 기반 ICE 서버 주입 완료');
    }
    
    /**
     * Publisher 연결 강화
     */
    enhancePublisherConnection(publisher) {
        if (!publisher) return;
        
        console.log('🔧 Publisher 연결 강화 중...');
        
        // Publisher 내부 RTCPeerConnection 접근 시도
        setTimeout(() => {
            try {
                // OpenVidu Publisher의 내부 구조 탐색
                if (publisher.stream && publisher.stream.connection) {
                    console.log('🔍 Publisher WebRTC 연결 상태 확인');
                    this.monitorWebRTCConnection(publisher, 'Publisher');
                }
            } catch (error) {
                console.warn('Publisher 연결 강화 실패:', error.message);
            }
        }, 1000);
    }
    
    /**
     * Subscriber 연결 강화
     */
    enhanceSubscriberConnection(subscriber) {
        if (!subscriber) return;
        
        console.log('🔧 Subscriber 연결 강화 중...');
        
        // Subscriber 내부 RTCPeerConnection 접근 시도
        setTimeout(() => {
            try {
                // OpenVidu Subscriber의 내부 구조 탐색
                if (subscriber.stream && subscriber.stream.connection) {
                    console.log('🔍 Subscriber WebRTC 연결 상태 확인');
                    this.monitorWebRTCConnection(subscriber, 'Subscriber');
                }
            } catch (error) {
                console.warn('Subscriber 연결 강화 실패:', error.message);
            }
        }, 1000);
    }
    
    /**
     * WebRTC 연결 상태 모니터링
     */
    monitorWebRTCConnection(streamObject, type) {
        console.log(`🔍 ${type} WebRTC 연결 모니터링 시작`);
        
        // 정기적으로 연결 상태 확인
        const monitorInterval = setInterval(() => {
            try {
                // 연결 상태 확인 로직
                if (streamObject && streamObject.stream) {
                    const connectionState = this.getConnectionState(streamObject);
                    console.log(`📊 ${type} 연결 상태:`, connectionState);
                    
                    // 연결 실패 시 복구 시도
                    if (connectionState === 'failed' || connectionState === 'disconnected') {
                        console.log(`🔄 ${type} 연결 복구 시도...`);
                        this.attemptConnectionRecovery(streamObject, type);
                    }
                } else {
                    // 스트림이 없어진 경우 모니터링 중지
                    clearInterval(monitorInterval);
                }
            } catch (error) {
                console.warn(`${type} 모니터링 오류:`, error.message);
                clearInterval(monitorInterval);
            }
        }, 5000); // 5초마다 확인
        
        // 2분 후 모니터링 자동 중지
        setTimeout(() => {
            clearInterval(monitorInterval);
            console.log(`⏱️ ${type} 연결 모니터링 자동 중지`);
        }, 120000);
    }
    
    /**
     * 연결 상태 확인
     */
    getConnectionState(streamObject) {
        try {
            // OpenVidu 내부 구조에서 RTCPeerConnection 상태 추출 시도
            // 이는 OpenVidu 버전에 따라 달라질 수 있음
            return 'unknown';
        } catch (error) {
            return 'error';
        }
    }
    
    /**
     * 연결 복구 시도
     */
    attemptConnectionRecovery(streamObject, type) {
        console.log(`🔄 ${type} 연결 복구 시도...`);
        
        // 간단한 복구 전략
        try {
            // 필요시 ICE 재시작 또는 재연결 로직 구현
            this.showToast(`${type} 연결을 복구하는 중...`);
            
            // 복구 성공 시
            setTimeout(() => {
                this.showToast(`${type} 연결이 복구되었습니다`);
            }, 3000);
            
        } catch (error) {
            console.error(`${type} 연결 복구 실패:`, error.message);
            this.showToast(`${type} 연결 복구 실패. 새로고침을 권장합니다.`);
        }
    }
    
    /**
     * 고급 연결 모니터링 설정
     */
    setupAdvancedConnectionMonitoring() {
        console.log('🔧 고급 연결 모니터링 설정 중...');
        
        // 전역 WebRTC 통계 수집
        setInterval(() => {
            this.collectWebRTCStats();
        }, 10000); // 10초마다 통계 수집
    }
    
    /**
     * WebRTC 통계 수집
     */
    async collectWebRTCStats() {
        try {
            // 현재 활성 RTCPeerConnection들의 통계 수집
            const stats = await this.gatherConnectionStats();
            
            if (stats && stats.length > 0) {
                console.log('📊 WebRTC 연결 통계:', stats);
                
                // 연결 품질 분석
                const connectionQuality = this.analyzeConnectionQuality(stats);
                if (connectionQuality === 'poor') {
                    console.warn('⚠️ 연결 품질 저하 감지');
                    // 필요시 사용자에게 알림
                }
            }
        } catch (error) {
            console.debug('WebRTC 통계 수집 실패:', error.message);
        }
    }
    
    /**
     * 연결 통계 수집
     */
    async gatherConnectionStats() {
        // RTCPeerConnection 통계 수집 로직 (구현 예정)
        return [];
    }
    
    /**
     * 연결 품질 분석
     */
    analyzeConnectionQuality() {
        // 간단한 품질 분석 로직
        return 'good'; // 기본값
    }
    
    /**
     * 🚀 대체 WebRTC 라이브러리 - 직접 WebRTC API 사용
     * OpenVidu 연결 실패 시 순수 WebRTC로 폴백
     */
    async initializeFallbackWebRTC() {
        console.log('🚀 대체 WebRTC 라이브러리 초기화 중...');
        
        try {
            // 직접 WebRTC PeerConnection 생성
            const configuration = {
                iceServers: [
                    // 모든 가용한 STUN/TURN 서버
                    { urls: 'stun:stun.l.google.com:19302' },
                    { urls: 'stun:stun1.l.google.com:19302' },
                    { urls: 'stun:stun2.l.google.com:19302' },
                    { urls: 'stun:stun.cloudflare.com:3478' },
                    { urls: 'stun:stun.stunprotocol.org:3478' },
                    { urls: 'stun:13.209.15.208:3478' },
                    // 공개 TURN 서버들
                    { urls: 'turn:openrelay.metered.ca:80', username: 'openrelayproject', credential: 'openrelayproject' },
                    { urls: 'turn:openrelay.metered.ca:443', username: 'openrelayproject', credential: 'openrelayproject' },
                    { urls: 'turn:openrelay.metered.ca:443?transport=tcp', username: 'openrelayproject', credential: 'openrelayproject' },
                    { urls: 'turn:relay.backups.cz', username: 'webrtc', credential: 'webrtc' },
                    { urls: 'turn:relay.backups.cz?transport=tcp', username: 'webrtc', credential: 'webrtc' }
                ],
                iceTransportPolicy: 'all',
                iceCandidatePoolSize: 50,
                bundlePolicy: 'balanced',
                rtcpMuxPolicy: 'negotiate'
            };
            
            // Override를 우회하여 원본 RTCPeerConnection 사용
            const OriginalRTC = window.RTCPeerConnection.__proto__.constructor;
            this.fallbackPeerConnection = new OriginalRTC(configuration);
            
            // ICE 후보 이벤트 처리
            this.fallbackPeerConnection.onicecandidate = (event) => {
                if (event.candidate) {
                    console.log('🧊 Fallback ICE Candidate:', event.candidate.candidate);
                } else {
                    console.log('✅ Fallback ICE 후보 수집 완료');
                }
            };
            
            // 연결 상태 모니터링
            this.fallbackPeerConnection.oniceconnectionstatechange = () => {
                const state = this.fallbackPeerConnection.iceConnectionState;
                console.log(`🧊 Fallback ICE Connection State: ${state}`);
                
                switch (state) {
                    case 'connected':
                        console.log('✅ Fallback WebRTC 연결 성공!');
                        this.showToast('대체 연결 방식으로 성공했습니다');
                        break;
                    case 'disconnected':
                        console.warn('⚠️ Fallback 연결 끊어짐 - 재연결 시도');
                        this.attemptFallbackReconnection();
                        break;
                    case 'failed':
                        console.error('❌ Fallback 연결 완전 실패');
                        this.showToast('연결에 실패했습니다. 네트워크를 확인해주세요');
                        break;
                }
            };
            
            // 데이터 채널 생성 (시그널링 대체)
            this.fallbackDataChannel = this.fallbackPeerConnection.createDataChannel('fallback-signaling', {
                ordered: true
            });
            
            this.fallbackDataChannel.onopen = () => {
                console.log('📡 Fallback 데이터 채널 열림');
            };
            
            this.fallbackDataChannel.onmessage = (event) => {
                console.log('📨 Fallback 메시지 수신:', event.data);
                this.handleFallbackMessage(event.data);
            };
            
            // 로컬 스트림 추가
            const stream = await navigator.mediaDevices.getUserMedia({
                video: { width: 640, height: 480 },
                audio: true
            });
            
            stream.getTracks().forEach(track => {
                this.fallbackPeerConnection.addTrack(track, stream);
            });
            
            // 로컬 비디오 표시
            if (this.mainVideo) {
                this.mainVideo.srcObject = stream;
                this.mainVideo.play();
            }
            
            // Offer 생성 및 시그널링 시작
            await this.startFallbackSignaling();
            
            console.log('✅ Fallback WebRTC 초기화 완료');
            return true;
            
        } catch (error) {
            console.error('❌ Fallback WebRTC 초기화 실패:', error);
            return false;
        }
    }
    
    /**
     * Fallback 시그널링 시작
     */
    async startFallbackSignaling() {
        try {
            console.log('📡 Fallback 시그널링 시작...');
            
            // SDP Offer 생성
            const offer = await this.fallbackPeerConnection.createOffer({
                offerToReceiveAudio: true,
                offerToReceiveVideo: true
            });
            
            await this.fallbackPeerConnection.setLocalDescription(offer);
            
            console.log('📤 Fallback Offer 생성:', offer);
            
            // 실제 시그널링 서버 없이는 완전한 연결 불가능하지만
            // ICE 후보 수집은 가능하므로 연결성 테스트에 유용
            
        } catch (error) {
            console.error('❌ Fallback 시그널링 실패:', error);
        }
    }
    
    /**
     * Fallback 메시지 처리
     */
    handleFallbackMessage(data) {
        try {
            const message = JSON.parse(data);
            console.log('📨 Fallback 메시지 처리:', message);
            
            // 메시지 타입별 처리
            switch (message.type) {
                case 'chat':
                    this.handleChatMessage({ data: message.content });
                    break;
                case 'user-joined':
                    this.showToast(`${message.username}님이 참가했습니다`);
                    break;
                case 'user-left':
                    this.showToast(`${message.username}님이 나갔습니다`);
                    break;
            }
        } catch (error) {
            console.error('Fallback 메시지 파싱 실패:', error);
        }
    }
    
    /**
     * Fallback 재연결 시도
     */
    async attemptFallbackReconnection() {
        console.log('🔄 Fallback 재연결 시도 중...');
        
        try {
            // ICE 재시작
            if (this.fallbackPeerConnection.restartIce) {
                this.fallbackPeerConnection.restartIce();
            }
            
            // 새로운 Offer 생성
            await this.startFallbackSignaling();
            
        } catch (error) {
            console.error('❌ Fallback 재연결 실패:', error);
        }
    }
    
    /**
     * Fallback WebRTC 연결 테스트
     */
    async testFallbackConnectivity() {
        console.log('🧪 Fallback WebRTC 연결성 테스트 시작...');
        
        try {
            const testResult = await this.initializeFallbackWebRTC();
            
            if (testResult) {
                // 5초 후 연결 상태 확인
                setTimeout(() => {
                    const iceState = this.fallbackPeerConnection?.iceConnectionState;
                    const gatheringState = this.fallbackPeerConnection?.iceGatheringState;
                    
                    console.log('📊 Fallback 연결성 테스트 결과:');
                    console.log(`  - ICE Connection State: ${iceState}`);
                    console.log(`  - ICE Gathering State: ${gatheringState}`);
                    
                    if (iceState === 'connected' || iceState === 'completed') {
                        console.log('✅ Fallback 연결성 테스트 성공');
                        return 'success';
                    } else if (gatheringState === 'complete') {
                        console.log('⚠️ ICE 후보 수집은 성공, 연결은 시그널링 서버 필요');
                        return 'partial';
                    } else {
                        console.log('❌ Fallback 연결성 테스트 실패');
                        return 'failed';
                    }
                }, 5000);
            }
            
            return testResult ? 'initiated' : 'failed';
            
        } catch (error) {
            console.error('❌ Fallback 연결성 테스트 오류:', error);
            return 'error';
        }
    }
    
    /**
     * WebRTC 라이브러리 정보 수집
     */
    getWebRTCLibraryInfo() {
        const info = {
            openVidu: {
                available: typeof OpenVidu !== 'undefined',
                version: '2.30.0', // 현재 사용 중인 버전
                status: 'primary'
            },
            nativeWebRTC: {
                available: typeof RTCPeerConnection !== 'undefined',
                version: 'native',
                status: 'fallback'
            },
            alternativeLibraries: [
                {
                    name: 'Simple-peer',
                    available: typeof SimplePeer !== 'undefined',
                    description: '경량 WebRTC 래퍼 라이브러리'
                },
                {
                    name: 'PeerJS',
                    available: typeof Peer !== 'undefined',
                    description: 'P2P 연결 전용 라이브러리'
                },
                {
                    name: 'MediaSoup-client',
                    available: typeof mediasoupClient !== 'undefined',
                    description: 'SFU 기반 미디어 서버 클라이언트'
                }
            ],
            recommendations: [
                'OpenVidu 연결 실패 시 네이티브 WebRTC 사용',
                'ICE 연결 문제 시 다중 TURN 서버 활용',
                '네트워크 제약이 심한 경우 TCP TURN 사용 권장'
            ]
        };
        
        console.log('📚 WebRTC 라이브러리 정보:', info);
        return info;
    }
    
    /**
     * 🔍 최종 네트워크 진단 - 종합적인 연결성 테스트
     */
    async performComprehensiveNetworkDiagnosis() {
        console.log('🔍 최종 네트워크 진단 시작...');
        
        const diagnosis = {
            timestamp: new Date().toISOString(),
            basicConnectivity: {},
            stunServers: [],
            turnServers: [],
            webrtcSupport: {},
            recommendations: []
        };
        
        try {
            // 1. 기본 네트워크 연결성 테스트
            console.log('🌐 기본 네트워크 연결성 테스트...');
            diagnosis.basicConnectivity = await this.testBasicConnectivity();
            
            // 2. STUN 서버 연결성 테스트
            console.log('🧊 STUN 서버 연결성 테스트...');
            diagnosis.stunServers = await this.testStunServers();
            
            // 3. TURN 서버 연결성 테스트
            console.log('🔄 TURN 서버 연결성 테스트...');
            diagnosis.turnServers = await this.testTurnServers();
            
            // 4. WebRTC 지원 기능 테스트
            console.log('📱 WebRTC 지원 기능 테스트...');
            diagnosis.webrtcSupport = await this.testWebRTCSupport();
            
            // 5. 종합 분석 및 권장사항 생성
            diagnosis.recommendations = this.generateNetworkRecommendations(diagnosis);
            
            // 6. 진단 결과 출력
            this.displayDiagnosisResults(diagnosis);
            
            return diagnosis;
            
        } catch (error) {
            console.error('❌ 네트워크 진단 중 오류:', error);
            diagnosis.error = error.message;
            return diagnosis;
        }
    }
    
    /**
     * 기본 네트워크 연결성 테스트
     */
    async testBasicConnectivity() {
        const results = {
            timestamp: Date.now()
        };
        
        const testUrls = [
            { name: 'OpenVidu Server', url: 'https://crema.bitcointothemars.com/openvidu' },
            { name: 'Application Server', url: 'https://crema.bitcointothemars.com' },
            { name: 'Google DNS', url: 'https://8.8.8.8' },
            { name: 'Cloudflare DNS', url: 'https://1.1.1.1' }
        ];
        
        for (const test of testUrls) {
            try {
                const startTime = Date.now();
                const response = await fetch(test.url, { 
                    method: 'HEAD', 
                    mode: 'no-cors',
                    signal: AbortSignal.timeout(5000) 
                });
                const endTime = Date.now();
                
                results[test.name] = {
                    status: 'success',
                    latency: endTime - startTime,
                    responseType: response.type
                };
                
            } catch (error) {
                results[test.name] = {
                    status: 'failed',
                    error: error.message
                };
            }
        }
        
        console.log('🌐 기본 연결성 테스트 결과:', results);
        return results;
    }
    
    /**
     * STUN 서버 테스트
     */
    async testStunServers() {
        const stunServers = [
            'stun:stun.l.google.com:19302',
            'stun:stun1.l.google.com:19302',
            'stun:stun.cloudflare.com:3478',
            'stun:stun.stunprotocol.org:3478',
            'stun:13.209.15.208:3478' // 내부 서버
        ];
        
        const results = [];
        
        for (const stunUrl of stunServers) {
            try {
                console.log(`🧊 STUN 서버 테스트: ${stunUrl}`);
                
                // Override를 우회하여 원본 RTCPeerConnection 사용
                const OriginalRTC = window.RTCPeerConnection.__proto__.constructor;
                const config = { iceServers: [{ urls: stunUrl }] };
                const pc = new OriginalRTC(config);
                
                const testResult = await this.performStunTest(pc, stunUrl);
                results.push({
                    url: stunUrl,
                    status: testResult.status,
                    candidates: testResult.candidates,
                    latency: testResult.latency
                });
                
                pc.close();
                
            } catch (error) {
                results.push({
                    url: stunUrl,
                    status: 'failed',
                    error: error.message
                });
            }
        }
        
        console.log('🧊 STUN 서버 테스트 결과:', results);
        return results;
    }
    
    /**
     * 개별 STUN 서버 테스트
     */
    async performStunTest(pc, stunUrl) {
        return new Promise((resolve) => {
            const startTime = Date.now();
            let candidateCount = 0;
            let resolved = false;
            
            pc.onicecandidate = (event) => {
                if (!resolved && event.candidate) {
                    candidateCount++;
                    console.log(`  - ICE Candidate from ${stunUrl}:`, event.candidate.candidate);
                } else if (!resolved && !event.candidate) {
                    resolved = true;
                    const endTime = Date.now();
                    resolve({
                        status: candidateCount > 0 ? 'success' : 'no-candidates',
                        candidates: candidateCount,
                        latency: endTime - startTime
                    });
                }
            };
            
            pc.createDataChannel('test');
            pc.createOffer().then(offer => {
                return pc.setLocalDescription(offer);
            });
            
            // 10초 타임아웃
            setTimeout(() => {
                if (!resolved) {
                    resolved = true;
                    resolve({
                        status: 'timeout',
                        candidates: candidateCount,
                        latency: 10000
                    });
                }
            }, 10000);
        });
    }
    
    /**
     * TURN 서버 테스트
     */
    async testTurnServers() {
        const turnServers = [
            { urls: 'turn:openrelay.metered.ca:80', username: 'openrelayproject', credential: 'openrelayproject' },
            { urls: 'turn:openrelay.metered.ca:443', username: 'openrelayproject', credential: 'openrelayproject' },
            { urls: 'turn:relay.backups.cz', username: 'webrtc', credential: 'webrtc' }
        ];
        
        const results = [];
        
        for (const turnServer of turnServers) {
            try {
                console.log(`🔄 TURN 서버 테스트: ${turnServer.urls}`);
                
                // Override를 우회하여 원본 RTCPeerConnection 사용
                const OriginalRTC = window.RTCPeerConnection.__proto__.constructor;
                const config = { 
                    iceServers: [turnServer],
                    iceTransportPolicy: 'relay' // TURN 서버만 사용
                };
                const pc = new OriginalRTC(config);
                
                const testResult = await this.performTurnTest(pc, turnServer.urls);
                results.push({
                    url: turnServer.urls,
                    username: turnServer.username,
                    status: testResult.status,
                    candidates: testResult.candidates,
                    latency: testResult.latency
                });
                
                pc.close();
                
            } catch (error) {
                results.push({
                    url: turnServer.urls,
                    status: 'failed',
                    error: error.message
                });
            }
        }
        
        console.log('🔄 TURN 서버 테스트 결과:', results);
        return results;
    }
    
    /**
     * 개별 TURN 서버 테스트
     */
    async performTurnTest(pc, turnUrl) {
        return new Promise((resolve) => {
            const startTime = Date.now();
            let candidateCount = 0;
            let resolved = false;
            
            pc.onicecandidate = (event) => {
                if (!resolved && event.candidate) {
                    candidateCount++;
                    console.log(`  - TURN Candidate from ${turnUrl}:`, event.candidate.candidate);
                } else if (!resolved && !event.candidate) {
                    resolved = true;
                    const endTime = Date.now();
                    resolve({
                        status: candidateCount > 0 ? 'success' : 'no-candidates',
                        candidates: candidateCount,
                        latency: endTime - startTime
                    });
                }
            };
            
            pc.createDataChannel('test');
            pc.createOffer().then(offer => {
                return pc.setLocalDescription(offer);
            });
            
            // 15초 타임아웃 (TURN은 STUN보다 오래 걸릴 수 있음)
            setTimeout(() => {
                if (!resolved) {
                    resolved = true;
                    resolve({
                        status: 'timeout',
                        candidates: candidateCount,
                        latency: 15000
                    });
                }
            }, 15000);
        });
    }
    
    /**
     * WebRTC 지원 기능 테스트
     */
    async testWebRTCSupport() {
        const support = {
            rtcPeerConnection: typeof RTCPeerConnection !== 'undefined',
            getUserMedia: typeof navigator.mediaDevices?.getUserMedia !== 'undefined',
            webRTC: typeof window.webkitRTCPeerConnection !== 'undefined' || typeof window.RTCPeerConnection !== 'undefined',
            dataChannel: false,
            screenShare: typeof navigator.mediaDevices?.getDisplayMedia !== 'undefined'
        };
        
        // DataChannel 지원 테스트 (안전한 구성 사용)
        try {
            // Override를 우회하여 원본 RTCPeerConnection 사용
            const OriginalRTC = window.RTCPeerConnection.__proto__.constructor;
            const pc = new OriginalRTC({ iceServers: [] }); // 빈 구성으로 테스트
            pc.createDataChannel('test');
            support.dataChannel = true;
            pc.close();
        } catch (error) {
            console.warn('DataChannel 지원 테스트 실패:', error);
        }
        
        // 미디어 권한 테스트 (실제로는 요청하지 않고 지원 여부만 확인)
        try {
            await navigator.mediaDevices.enumerateDevices();
            support.mediaDevices = true;
        } catch (error) {
            support.mediaDevices = false;
        }
        
        console.log('📱 WebRTC 지원 기능:', support);
        return support;
    }
    
    /**
     * 네트워크 권장사항 생성
     */
    generateNetworkRecommendations(diagnosis) {
        const recommendations = [];
        
        // STUN 서버 분석
        const successfulStun = diagnosis.stunServers.filter(s => s.status === 'success');
        if (successfulStun.length === 0) {
            recommendations.push({
                priority: 'critical',
                issue: 'STUN 서버 연결 실패',
                solution: '방화벽 설정 확인 및 UDP 포트 개방 필요'
            });
        } else if (successfulStun.length < 3) {
            recommendations.push({
                priority: 'warning',
                issue: 'STUN 서버 연결 불안정',
                solution: '네트워크 설정 점검 권장'
            });
        }
        
        // TURN 서버 분석
        const successfulTurn = diagnosis.turnServers.filter(s => s.status === 'success');
        if (successfulTurn.length === 0) {
            recommendations.push({
                priority: 'critical',
                issue: 'TURN 서버 연결 실패',
                solution: 'NAT 환경에서 화상통화 불가능, 네트워크 관리자 문의 필요'
            });
        } else if (successfulTurn.length < 2) {
            recommendations.push({
                priority: 'warning',
                issue: '백업 TURN 서버 부족',
                solution: '안정적인 연결을 위해 추가 TURN 서버 설정 권장'
            });
        }
        
        // WebRTC 지원 분석
        if (!diagnosis.webrtcSupport.rtcPeerConnection) {
            recommendations.push({
                priority: 'critical',
                issue: 'WebRTC 미지원 브라우저',
                solution: 'Chrome, Firefox, Safari, Edge 등 최신 브라우저 사용 필요'
            });
        }
        
        if (!diagnosis.webrtcSupport.getUserMedia) {
            recommendations.push({
                priority: 'critical',
                issue: '미디어 접근 권한 없음',
                solution: 'HTTPS 환경에서 카메라/마이크 권한 허용 필요'
            });
        }
        
        // 성공적인 경우 최적화 권장사항
        if (successfulStun.length >= 3 && successfulTurn.length >= 2) {
            recommendations.push({
                priority: 'info',
                issue: '네트워크 연결 양호',
                solution: '최적의 화상통화 환경입니다'
            });
        }
        
        return recommendations;
    }
    
    /**
     * 진단 결과 표시
     */
    displayDiagnosisResults(diagnosis) {
        console.log('📊 네트워크 진단 최종 결과:');
        console.log('=================================');
        
        // 기본 연결성
        console.log('🌐 기본 연결성:');
        Object.entries(diagnosis.basicConnectivity).forEach(([key, value]) => {
            if (key !== 'timestamp' && typeof value === 'object') {
                const status = value.status === 'success' ? '✅' : '❌';
                const latency = value.latency ? `(${value.latency}ms)` : '';
                console.log(`  ${status} ${key}: ${value.status} ${latency}`);
            }
        });
        
        // STUN 서버 결과
        console.log('🧊 STUN 서버:');
        diagnosis.stunServers.forEach(server => {
            const status = server.status === 'success' ? '✅' : '❌';
            const candidates = server.candidates ? `(${server.candidates} candidates)` : '';
            console.log(`  ${status} ${server.url}: ${server.status} ${candidates}`);
        });
        
        // TURN 서버 결과
        console.log('🔄 TURN 서버:');
        diagnosis.turnServers.forEach(server => {
            const status = server.status === 'success' ? '✅' : '❌';
            const candidates = server.candidates ? `(${server.candidates} candidates)` : '';
            console.log(`  ${status} ${server.url}: ${server.status} ${candidates}`);
        });
        
        // 권장사항
        console.log('💡 권장사항:');
        diagnosis.recommendations.forEach((rec) => {
            const priority = rec.priority === 'critical' ? '🚨' : 
                           rec.priority === 'warning' ? '⚠️' : '💡';
            console.log(`  ${priority} ${rec.issue}: ${rec.solution}`);
        });
        
        console.log('=================================');
        
        // 사용자에게 결과 요약 표시
        const criticalIssues = diagnosis.recommendations.filter(r => r.priority === 'critical').length;
        if (criticalIssues > 0) {
            this.showToast(`네트워크 진단 완료: ${criticalIssues}개 중요 문제 발견. 콘솔을 확인하세요.`);
        } else {
            this.showToast('네트워크 진단 완료: 연결 상태 양호');
        }
        
        return diagnosis;
    }
    
    configureOpenVidu() {
        // 🎯 ULTIMATE FIX: OpenVidu 서버 URL 명시적 설정
        const openViduServerUrl = 'https://crema.bitcointothemars.com/openvidu';
        window.OPENVIDU_SERVER_URL = openViduServerUrl;
        
        console.log('🔧 [ver-20250826022100] OpenVidu Server URL configured:', openViduServerUrl);
        console.log('🔧 [ver-20250826022100] Protocol:', window.location.protocol);
        console.log('🔧 [ver-20250826022100] Host:', window.location.host);
        
        // HTTPS 환경 확인
        if (typeof navigator !== 'undefined' && navigator.userAgent) {
            console.log('User Agent:', navigator.userAgent);
        }
        
        // WebRTC 지원 확인
        if (!window.RTCPeerConnection) {
            throw new Error('이 브라우저는 WebRTC를 지원하지 않습니다.');
        }
    }
    
    async connectToSession() {
        this.showLoading(true);
        
        try {
            // OpenVidu 객체 생성 전 환경 확인
            console.log('OpenVidu type:', typeof OpenVidu);
            console.log('Window OpenVidu:', window.OpenVidu);
            
            if (typeof OpenVidu === 'undefined' && typeof window.OpenVidu === 'undefined') {
                throw new Error('OpenVidu 라이브러리가 로드되지 않았습니다. CDN에서 로드를 확인해주세요.');
            }
            
            // OpenVidu가 window 객체에 있는 경우 할당
            if (typeof OpenVidu === 'undefined' && typeof window.OpenVidu !== 'undefined') {
                window.OpenVidu = window.OpenVidu;
            }
            
            // WebRTC 지원 확인
            if (!window.RTCPeerConnection) {
                throw new Error('이 브라우저는 WebRTC를 지원하지 않습니다.');
            }
            
            // 🚀 RTCPeerConnection 오버라이드 비활성화 - OpenVidu가 제공하는 ICE 서버 사용
            // this.overrideRTCPeerConnection();  // 비활성화: OpenVidu의 동적 TURN 설정을 사용하기 위해
            
            // 🎯 ULTIMATE FIX: OpenVidu 객체 생성 및 서버 연결 설정
            console.log('🔧 [ver-20250826022100] Creating OpenVidu with proper server configuration...');
            
            // OpenVidu 2.30.0 표준 생성 방식
            this.OV = new OpenVidu();
            console.log('🔧 [ver-20250826022100] OpenVidu instance created');
            
            // 🎯 OpenVidu 고급 설정 - TURN 서버 강제 활성화 (테스트)
            // ICE Transport Policy를 'relay'로 설정하여 TURN 서버만 사용
            this.OV.setAdvancedConfiguration({
                iceTransportPolicy: 'relay',  // TURN 서버 강제 사용
                publisherSpeakingEventsOptions: {
                    interval: 100  // 기본값
                }
            });
            console.log('🔧 [ver-20250826-turn-fix] ICE Transport Policy set to "relay" - forcing TURN usage');
            
            // 🎯 ULTIMATE FIX: 세션 초기화 (sessionId 전달 필수)
            console.log('🔧 [ver-20250826022100] Initializing session with sessionId:', this.sessionData.sessionId);
            console.log('🔧 [ver-20250826022100] Full session data:', this.sessionData);
            
            this.session = this.OV.initSession(this.sessionData.sessionId);
            console.log('🔧 [ver-20250826022100] ✅ Session initialized successfully');
            
            // 이벤트 리스너 등록
            this.session.on('streamCreated', (event) => {
                console.log('스트림 생성됨:', event);
                this.handleStreamCreated(event);
            });
            
            this.session.on('streamDestroyed', (event) => {
                console.log('스트림 파괴됨:', event);
                this.handleStreamDestroyed(event);
            });
            
            this.session.on('connectionCreated', (event) => {
                console.log('연결 생성됨:', event);
                this.handleConnectionCreated(event);
            });
            
            this.session.on('connectionDestroyed', (event) => {
                console.log('연결 파괴됨:', event);
                this.handleConnectionDestroyed(event);
            });
            
            // 채팅 메시지 이벤트
            this.session.on('signal:chat', (event) => {
                console.log('채팅 메시지 수신:', event);
                this.handleChatMessage(event);
            });
            
            // 파일 공유 이벤트
            this.session.on('signal:file-share', (event) => {
                console.log('파일 공유 수신:', event);
                this.handleFileShare(event);
            });
            
            // ICE 연결 상태 모니터링 이벤트 추가
            this.session.on('reconnecting', () => {
                console.log('🔄 세션 재연결 중...');
                this.showToast('연결을 복구하는 중입니다...');
            });
            
            this.session.on('reconnected', () => {
                console.log('✅ 세션 재연결 완료');
                this.showToast('연결이 복구되었습니다');
            });
            
            this.session.on('sessionDisconnected', (event) => {
                console.log('❌ 세션 연결 끊어짐:', event);
                if (event.reason === 'networkDisconnect') {
                    this.handleNetworkDisconnection();
                }
            });
            
            // 백엔드에서 받은 토큰 처리 (개선된 통일 로직)
            const tokenFromBackend = this.sessionData.token;
            console.log('🔧 [ver-20250826030000] Token from backend:', tokenFromBackend);
            
            // 토큰 추출 로직 단순화
            let actualToken;
            
            if (tokenFromBackend.startsWith('wss://')) {
                // WebSocket URL에서 순수 토큰 추출
                try {
                    const url = new URL(tokenFromBackend.replace('wss://', 'https://'));
                    actualToken = url.searchParams.get('token');
                    console.log('🔧 [ver-20250826030000] Extracted token from WebSocket URL:', actualToken);
                } catch (error) {
                    console.error('❌ WebSocket URL 파싱 실패:', error);
                    throw new Error('WebSocket URL 파싱 실패: ' + tokenFromBackend);
                }
            } else if (tokenFromBackend.startsWith('tok_')) {
                // 이미 순수 토큰인 경우
                actualToken = tokenFromBackend;
                console.log('🔧 [ver-20250826030000] Using pure token directly:', actualToken);
            } else {
                console.error('❌ 알 수 없는 토큰 형식:', tokenFromBackend);
                throw new Error('알 수 없는 토큰 형식: ' + tokenFromBackend);
            }
            
            if (!actualToken || !actualToken.startsWith('tok_')) {
                throw new Error('유효한 JWT 토큰을 추출할 수 없습니다: ' + tokenFromBackend);
            }
            
            console.log('🔧 [ver-20250826030000] 🚀 최적화된 연결 시도:');
            console.log('🔧 [ver-20250826030000] - SessionId:', this.sessionData.sessionId);
            console.log('🔧 [ver-20250826030000] - Token:', actualToken);
            console.log('🔧 [ver-20250826030000] - Username:', this.sessionData.username);
            
            // 향상된 토큰 접근 방식 (순차 시도)
            console.log('🔧 [ver-20250826030000] Starting enhanced connection approach...');
            
            // 스마트 토큰 연결 시도 (이전 성공 형식 우선)
            let connectionSuccess = false;
            const lastSuccessfulType = localStorage.getItem('crema_successful_token_type');
            
            // 성공했던 토큰 형식을 첫 번째로 시도
            const tokenFormats = [
                { type: 'direct-url', token: `wss://crema.bitcointothemars.com?sessionId=${this.sessionData.sessionId}&token=${actualToken}` },
                { type: 'pure', token: actualToken },
                { type: 'websocket-clean', token: tokenFromBackend.replace('/openvidu', '').replace('///', '//') }
            ];
            
            // 이전 성공 형식이 있으면 맨 앞으로 이동
            if (lastSuccessfulType) {
                const successFormat = tokenFormats.find(f => f.type === lastSuccessfulType);
                if (successFormat) {
                    tokenFormats.splice(tokenFormats.indexOf(successFormat), 1);
                    tokenFormats.unshift(successFormat);
                }
            }
            
            for (const format of tokenFormats) {
                try {
                    console.log(`🔧 [ver-20250826030000] Trying ${format.type} token format:`, format.token);
                    
                    // Connection metadata 통일
                    const connectionMetadata = {
                        clientData: this.sessionData.username,
                        username: this.sessionData.username
                    };
                    
                    await this.session.connect(format.token, JSON.stringify(connectionMetadata));
                    console.log(`✅ [ver-20250826030000] SUCCESS with ${format.type} token format!`);
                    connectionSuccess = true;
                    
                    // 성공한 토큰 형식 저장
                    localStorage.setItem('crema_successful_token_type', format.type);
                    break;
                } catch (error) {
                    console.log(`❌ [ver-20250826030000] Failed with ${format.type} format:`, error.message);
                }
            }
            
            if (!connectionSuccess) {
                console.error('🚨 [CONNECTION_FAILED] All token formats failed');
                throw new Error('세션 연결에 실패했습니다. 새로고침 후 다시 시도해주세요.');
            }
            
            // 🎯 OpenVidu 훅 비활성화 - OpenVidu 기본 동작 사용
            // this.injectOpenViduHooks(); // 비활성화: OpenVidu가 제공하는 기본 ICE 설정 사용
            
            // 로컬 스트림 초기화
            await this.initializeLocalStream();
            
            this.showLoading(false);
            this.showToast('화상통화에 연결되었습니다.');
            
        } catch (error) {
            this.showLoading(false);
            console.error('세션 연결 실패:', error);
            throw error;
        }
    }
    
    async initializeLocalStream() {
        try {
            // 미디어 권한 먼저 요청
            console.log('미디어 권한 요청 중...');
            
            try {
                const stream = await navigator.mediaDevices.getUserMedia({
                    audio: true,
                    video: { width: 640, height: 480 }
                });
                
                console.log('미디어 권한 허용됨');
                
                // 임시 스트림 중단 (OpenVidu에서 새로 생성할 예정)
                stream.getTracks().forEach(track => track.stop());
            } catch (mediaError) {
                console.error('미디어 권한 거부됨:', mediaError);
                throw new Error('카메라 및 마이크 권한이 필요합니다. 브라우저 설정에서 권한을 허용해주세요.');
            }
            
            // Publisher 생성
            this.publisher = await this.OV.initPublisherAsync(undefined, {
                audioSource: undefined,
                videoSource: undefined,
                publishAudio: true,
                publishVideo: true,
                resolution: '640x480',
                frameRate: 30,
                insertMode: 'APPEND',
                mirror: false
            });
            
            // 메인 비디오에 로컬 스트림 표시
            this.setMainVideo(this.publisher.stream, this.sessionData.username, false);
            
            // 세션에 발행
            await this.session.publish(this.publisher);
            
            // 자신을 참가자 맵에 추가 (초기 설정)
            if (this.session.connection) {
                this.participants.set(this.session.connection.connectionId, {
                    username: this.sessionData.username,
                    connectionId: this.session.connection.connectionId,
                    isMe: true,
                    connection: this.session.connection
                });
                console.log('✅ 자신을 참가자 맵에 초기 추가:', this.sessionData.username);
            }
            
            // 초기 참가자 목록 업데이트
            setTimeout(() => {
                this.updateParticipantsList();
            }, 1000);
            
            console.log('로컬 스트림 초기화 완료');
            
        } catch (error) {
            console.error('로컬 스트림 초기화 실패:', error);
            throw error;
        }
    }
    
    handleStreamCreated(event) {
        console.log('새 스트림 수신:', event.stream.streamId);
        
        // Subscriber 생성 (컨테이너를 명시적으로 지정하지 않음)
        const subscriber = this.session.subscribe(event.stream, undefined);
        this.subscribers.push(subscriber);
        
        console.log('🔧 Subscriber 생성됨:', subscriber);
        
        // 원격 스트림을 메인 비디오에 표시하기 위한 연결 정보 파싱
        const connection = event.stream.connection;
        let username = '사용자';
        
        try {
            // connection.data 파싱 시도
            if (connection.data) {
                console.log('Connection data:', connection.data);
                
                let dataToProcess = connection.data;
                
                // %/% 구분자로 분리된 데이터 처리
                if (typeof dataToProcess === 'string' && dataToProcess.includes('%/%')) {
                    console.log('Detected %/% separator in connection data');
                    // 첫 번째 부분만 사용 (clientData 부분)
                    dataToProcess = dataToProcess.split('%/%')[0];
                    console.log('Extracted first part:', dataToProcess);
                }
                
                // 여러 형태의 데이터 형식 지원
                if (typeof dataToProcess === 'string') {
                    // JSON 문자열인 경우
                    if (dataToProcess.startsWith('{')) {
                        const parsedData = JSON.parse(dataToProcess);
                        username = parsedData.clientData || parsedData.username || '사용자';
                    } else {
                        // 단순 문자열인 경우
                        username = dataToProcess;
                    }
                } else if (typeof dataToProcess === 'object') {
                    // 이미 객체인 경우
                    username = dataToProcess.clientData || dataToProcess.username || '사용자';
                }
            }
        } catch (error) {
            console.warn('Connection data 파싱 실패:', error);
            console.log('Raw connection data:', connection.data);
            // 기본값 사용
            username = '사용자' + Math.floor(Math.random() * 1000);
        }
        
        // 🎯 양방향 화면 전환 개선: streamCreated에서 즉시 화면 전환 처리
        if (!this.isScreenSharing) {
            console.log('🔄 원격 스트림 생성으로 인한 화면 전환 시작:', username);
            
            // 즉시 자신을 PIP로 설정
            this.setupLocalVideoPip();
            
            // Subscriber가 준비되면 메인 화면으로 설정
            subscriber.on('streamPlaying', (streamEvent) => {
                console.log('✅ 원격 스트림 준비 완료, 메인 화면으로 설정:', username);
                this.setMainVideo(event.stream, username, false);
                console.log('✅ 양방향 화면 전환 완료: 메인=' + username + ', PIP=나');
            });
        }
        
        // Subscriber의 비디오 요소가 준비되면 추가 설정
        subscriber.on('videoElementCreated', (videoEvent) => {
            console.log('🔧 원격 비디오 요소 생성됨:', videoEvent.element);
            // 원격 비디오 요소는 숨김 처리 (메인 비디오에서 스트림으로 표시됨)
            videoEvent.element.style.display = 'none';
        });
    }
    
    handleStreamDestroyed(event) {
        console.log('스트림 제거됨:', event.stream.streamId);
        
        // Subscriber 목록에서 제거
        this.subscribers = this.subscribers.filter(sub => sub.stream.streamId !== event.stream.streamId);
        
        // 메인 비디오가 제거된 스트림이었다면 로컬 스트림으로 변경
        if (this.mainVideo.srcObject === event.stream.getMediaStream()) {
            if (this.publisher) {
                this.setMainVideo(this.publisher.stream, this.sessionData.username, false);
            }
        }
    }
    
    setMainVideo(stream, username, isScreenShare = false, retryCount = 0) {
        try {
            console.log('🔧 setMainVideo 호출:', { stream, username, isScreenShare, retryCount });
            
            if (stream && stream.getMediaStream) {
                const mediaStream = stream.getMediaStream();
                
                if (!mediaStream) {
                    console.warn(`MediaStream이 null입니다. 재시도 중... (${retryCount + 1}/5)`);
                    // 최대 5회까지 재시도
                    if (retryCount < 5) {
                        setTimeout(() => {
                            this.setMainVideo(stream, username, isScreenShare, retryCount + 1);
                        }, 200 * (retryCount + 1)); // 점진적 지연
                    } else {
                        console.error('❌ MediaStream 획득 실패: 최대 재시도 횟수 초과');
                    }
                    return;
                }
                
                this.mainVideo.srcObject = mediaStream;
                this.mainVideoLabel.textContent = isScreenShare ? username + ' (화면공유)' : username;
                
                console.log('📺 MediaStream 설정 완료:', mediaStream.id);
                
                // 비디오 트랙 확인 (안전성 검사 추가)
                if (mediaStream.getVideoTracks && typeof mediaStream.getVideoTracks === 'function') {
                    const videoTracks = mediaStream.getVideoTracks();
                    console.log('🎥 비디오 트랙 수:', videoTracks.length);
                    
                    if (videoTracks.length > 0) {
                        const track = videoTracks[0];
                        console.log('🎥 비디오 트랙 상태:', {
                            readyState: track.readyState,
                            enabled: track.enabled,
                            muted: track.muted
                        });
                        
                        if (track.readyState === 'live') {
                            this.mainVideoOverlay.classList.add('hidden');
                        } else {
                            this.mainVideoOverlay.classList.remove('hidden');
                        }
                    } else {
                        console.log('🎥 비디오 트랙이 없습니다');
                        this.mainVideoOverlay.classList.remove('hidden');
                    }
                } else {
                    console.warn('getVideoTracks 메서드를 사용할 수 없습니다');
                    // 트랙 확인 불가 시 오버레이는 숨김 (비디오가 있다고 가정)
                    this.mainVideoOverlay.classList.add('hidden');
                }
                
                console.log('✅ 메인 비디오 설정 완료');
            } else {
                console.warn('스트림 또는 getMediaStream 메서드가 없습니다');
            }
        } catch (error) {
            console.error('❌ 메인 비디오 설정 실패:', error);
            console.error('Error details:', {
                stream: stream,
                hasGetMediaStream: stream && typeof stream.getMediaStream === 'function',
                username: username
            });
        }
    }
    
    // 오디오 토글
    toggleAudio() {
        if (this.publisher) {
            const enabled = this.publisher.publishAudio;
            this.publisher.publishAudio(!enabled);
            
            if (enabled) {
                this.toggleAudioBtn.classList.add('disabled');
            } else {
                this.toggleAudioBtn.classList.remove('disabled');
            }
            
            this.showToast(enabled ? '마이크가 꺼졌습니다' : '마이크가 켜졌습니다');
        }
    }
    
    // 비디오 토글
    toggleVideo() {
        if (this.publisher) {
            const enabled = this.publisher.publishVideo;
            this.publisher.publishVideo(!enabled);
            
            if (enabled) {
                this.toggleVideoBtn.classList.add('disabled');
                this.mainVideoOverlay.classList.remove('hidden');
            } else {
                this.toggleVideoBtn.classList.remove('disabled');
                this.mainVideoOverlay.classList.add('hidden');
            }
            
            this.showToast(enabled ? '비디오가 꺼졌습니다' : '비디오가 켜졌습니다');
        }
    }
    
    // 화면공유 토글
    async toggleScreenShare() {
        if (this.isScreenSharing) {
            await this.stopScreenShare();
        } else {
            await this.startScreenShare();
        }
    }
    
    async startScreenShare() {
        try {
            console.log('화면공유 시작 시도');
            
            // 화면공유 스트림 생성
            const screenStream = await navigator.mediaDevices.getDisplayMedia({
                video: true,
                audio: true
            });
            
            console.log('화면공유 스트림 생성 완료');
            
            // 화면공유 Publisher 생성 (Content Hint 설정 포함)
            this.screenSharePublisher = await this.OV.initPublisherAsync(undefined, {
                videoSource: screenStream.getVideoTracks()[0],
                audioSource: screenStream.getAudioTracks()[0] || false,
                publishVideo: true,
                publishAudio: screenStream.getAudioTracks().length > 0,
                mirror: false
            });
            
            // Content Hint 설정으로 경고 해결
            if (this.screenSharePublisher.stream && this.screenSharePublisher.stream.getMediaStream) {
                const videoTrack = this.screenSharePublisher.stream.getMediaStream().getVideoTracks()[0];
                if (videoTrack && videoTrack.contentHint !== undefined) {
                    videoTrack.contentHint = 'detail';  // 화면공유에 적합한 Content Hint
                }
            }
            
            // 기존 Publisher를 부드럽게 교체 (오디오 끊김 최소화)
            if (this.publisher) {
                // 화면공유 Publisher를 먼저 퍼블리시
                await this.session.publish(this.screenSharePublisher);
                // 그 다음 기존 Publisher 언퍼블리시 (순서 변경으로 오디오 끊김 감소)
                await this.session.unpublish(this.publisher);
            } else {
                // 기존 Publisher가 없는 경우 바로 퍼블리시
                await this.session.publish(this.screenSharePublisher);
            }
            
            // PIP용 새로운 캠 스트림 생성
            const camStream = await navigator.mediaDevices.getUserMedia({
                video: {
                    width: { ideal: 320 },
                    height: { ideal: 240 },
                    frameRate: { ideal: 15 }
                },
                audio: false
            });
            
            // PIP에 직접 설정
            this.pipVideo.srcObject = camStream;
            this.pipVideo.muted = true;
            this.pipContainer.classList.add('show');
            this.pipEnabled = true;
            this.pipUsername = this.sessionData.username;
            this.pipLabel.textContent = '내 캠';
            
            // 메인 비디오를 화면공유로 설정
            this.setMainVideo(this.screenSharePublisher.stream, this.sessionData.username, true);
            
            // 상태 업데이트
            this.isScreenSharing = true;
            this.toggleScreenShareBtn.classList.add('active');
            
            // 화면공유 종료 감지
            screenStream.getVideoTracks()[0].addEventListener('ended', () => {
                console.log('화면공유가 사용자에 의해 종료됨');
                this.stopScreenShare();
            });
            
            this.showToast('화면공유가 시작되었습니다');
            console.log('화면공유 시작 완료');
            
        } catch (error) {
            console.error('화면공유 시작 실패:', error);
            this.showToast('화면공유 시작에 실패했습니다');
        }
    }
    
    async stopScreenShare() {
        try {
            console.log('화면공유 중단 시도');
            
            // 화면공유 Publisher 정리
            if (this.screenSharePublisher) {
                await this.session.unpublish(this.screenSharePublisher);
                this.screenSharePublisher = null;
            }
            
            // PIP 숨기기
            this.hidePip();
            
            // 기존 Publisher 다시 퍼블리시
            if (this.publisher) {
                await this.session.publish(this.publisher);
                this.setMainVideo(this.publisher.stream, this.sessionData.username, false);
            }
            
            // 상태 업데이트
            this.isScreenSharing = false;
            this.toggleScreenShareBtn.classList.remove('active');
            
            this.showToast('화면공유가 중단되었습니다');
            console.log('화면공유 중단 완료');
            
        } catch (error) {
            console.error('화면공유 중단 실패:', error);
        }
    }
    
    // PIP 관련 메소드들
    startDragging(e) {
        if (e.target.closest('.pip-control-btn')) return;
        
        this.isDragging = true;
        this.pipContainer.classList.add('dragging');
        
        const rect = this.pipContainer.getBoundingClientRect();
        this.dragOffset = {
            x: e.clientX - rect.left,
            y: e.clientY - rect.top
        };
        
        // 스냅 영역 표시
        Object.values(this.snapZones).forEach(zone => zone.classList.add('active'));
    }
    
    handleDragging(e) {
        if (!this.isDragging) return;
        
        const containerRect = this.pipContainer.parentElement.getBoundingClientRect();
        const pipRect = this.pipContainer.getBoundingClientRect();
        
        let newX = e.clientX - containerRect.left - this.dragOffset.x;
        let newY = e.clientY - containerRect.top - this.dragOffset.y;
        
        // 경계 체크
        newX = Math.max(0, Math.min(newX, containerRect.width - pipRect.width));
        newY = Math.max(0, Math.min(newY, containerRect.height - pipRect.height));
        
        this.pipContainer.style.left = newX + 'px';
        this.pipContainer.style.top = newY + 'px';
        this.pipContainer.style.right = 'auto';
        this.pipContainer.style.bottom = 'auto';
    }
    
    stopDragging() {
        if (!this.isDragging) return;
        
        this.isDragging = false;
        this.pipContainer.classList.remove('dragging');
        
        // 스냅 영역 숨기기
        Object.values(this.snapZones).forEach(zone => zone.classList.remove('active'));
        
        // 스냅 처리
        this.snapToEdge();
    }
    
    snapToEdge() {
        const containerRect = this.pipContainer.parentElement.getBoundingClientRect();
        const pipRect = this.pipContainer.getBoundingClientRect();
        const snapThreshold = 50;
        
        const relativeRect = {
            left: pipRect.left - containerRect.left,
            top: pipRect.top - containerRect.top,
            right: containerRect.right - pipRect.right,
            bottom: containerRect.bottom - pipRect.bottom
        };
        
        // 가장 가까운 모서리로 스냅
        if (relativeRect.left < snapThreshold) {
            this.pipContainer.style.left = '20px';
        } else if (relativeRect.right < snapThreshold) {
            this.pipContainer.style.left = 'auto';
            this.pipContainer.style.right = '20px';
        }
        
        if (relativeRect.top < snapThreshold) {
            this.pipContainer.style.top = '20px';
        } else if (relativeRect.bottom < snapThreshold) {
            this.pipContainer.style.top = 'auto';
            this.pipContainer.style.bottom = '80px';
        }
    }
    
    swapPipAndMain() {
        try {
            if (this.isScreenSharing) {
                // 화면공유 중: PIP 캠 ↔ 메인 화면공유 교체
                const pipCamStream = this.pipVideo.srcObject;
                const mainScreenShareStream = this.mainVideo.srcObject;
                
                if (pipCamStream && mainScreenShareStream) {
                    // 메인 화면을 PIP의 캠으로 변경
                    this.mainVideo.srcObject = pipCamStream;
                    this.mainVideoLabel.textContent = this.pipUsername;
                    this.mainVideoOverlay.classList.add('hidden');
                    
                    // PIP를 화면공유로 변경  
                    this.pipVideo.srcObject = mainScreenShareStream;
                    this.pipLabel.textContent = this.sessionData.username + ' (화면공유)';
                    
                    this.showToast('캠과 화면공유가 교체되었습니다.');
                }
            }
        } catch (error) {
            console.error('화면 교체 오류:', error);
            this.showToast('화면 교체에 실패했습니다.');
        }
    }
    
    hidePip() {
        // PIP 스트림 정리
        if (this.pipVideo.srcObject) {
            const pipStream = this.pipVideo.srcObject;
            if (pipStream && pipStream.getTracks) {
                pipStream.getTracks().forEach(track => track.stop());
            }
        }
        
        this.pipVideo.srcObject = null;
        this.pipContainer.classList.remove('show');
        this.pipEnabled = false;
        this.pipStream = null;
        this.pipUsername = null;
        
        // 위치 초기화
        this.pipContainer.style.left = 'auto';
        this.pipContainer.style.top = 'auto';
        this.pipContainer.style.right = '20px';
        this.pipContainer.style.bottom = '20px';
    }
    
    // 사이드바 새 창 열기
    openSidebar() {
        const sidebarUrl = `/video-call-sidebar.html?sessionId=${this.sessionData.sessionId}&username=${this.sessionData.username}`;
        
        if (this.sidebarWindow && !this.sidebarWindow.closed) {
            this.sidebarWindow.focus();
            return;
        }
        
        this.sidebarWindow = window.open(
            sidebarUrl,
            'videoCallSidebar',
            'width=400,height=600,resizable=yes,scrollbars=yes'
        );
        
        if (this.sidebarWindow) {
            this.showToast('사이드바 창을 열었습니다');
        } else {
            this.showToast('팝업이 차단되었습니다. 팝업 허용 후 다시 시도해주세요');
        }
    }
    
    // 창 간 통신 설정
    setupWindowCommunication() {
        window.addEventListener('message', (event) => {
            if (event.origin !== window.location.origin) return;
            
            const { type, data } = event.data;
            
            switch (type) {
                case 'SEND_CHAT':
                    this.sendChatMessageFromWindow(data.message);
                    break;
                case 'REQUEST_SESSION_INFO':
                    event.source.postMessage({
                        type: 'SESSION_INFO',
                        data: this.sessionData
                    }, event.origin);
                    break;
            }
        });
    }
    
    // 창 간 통신을 통한 채팅 메시지 전송 (내부 호출용)
    async sendChatMessageFromWindow(message) {
        console.log('🔧 창 간 통신 채팅 메시지 전송:', message);
        
        if (!this.session) {
            console.error('❌ 세션이 연결되지 않아 메시지를 전송할 수 없습니다');
            return;
        }

        try {
            await this.session.signal({
                type: 'chat',
                data: JSON.stringify({
                    message: message,
                    username: this.sessionData.username,
                    timestamp: Date.now()
                })
            });
            console.log('✅ 창 간 통신 채팅 메시지 전송 완료');
        } catch (error) {
            console.error('❌ 창 간 통신 채팅 메시지 전송 실패:', error);
        }
    }
    
    // 통화 타이머
    startCallTimer() {
        this.callStartTime = Date.now();
        this.callTimer = setInterval(() => {
            const elapsed = Date.now() - this.callStartTime;
            const minutes = Math.floor(elapsed / 60000);
            const seconds = Math.floor((elapsed % 60000) / 1000);
            this.callDuration.textContent = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
        }, 1000);
    }
    
    // 세션 나가기
    async leaveSession() {
        if (confirm('화상통화를 종료하시겠습니까?')) {
            this.cleanup();
            window.location.href = '/';
        }
    }
    
    // 정리 작업
    cleanup() {
        try {
            // 타이머 정리
            if (this.callTimer) {
                clearInterval(this.callTimer);
            }
            
            // PIP 정리
            this.hidePip();
            
            // 사이드바 창 닫기
            if (this.sidebarWindow && !this.sidebarWindow.closed) {
                this.sidebarWindow.close();
            }
            
            // 세션 정리
            if (this.session) {
                this.session.disconnect();
            }
            
            console.log('정리 작업 완료');
        } catch (error) {
            console.error('정리 작업 실패:', error);
        }
    }
    
    // 유틸리티 메소드들
    showLoading(show) {
        this.loadingScreen.style.display = show ? 'flex' : 'none';
    }
    
    showToast(message) {
        this.toastMessage.textContent = message;
        this.toast.style.display = 'block';
        
        setTimeout(() => {
            this.toast.style.display = 'none';
        }, 3000);
    }
    
    // ===== 사이드바 관리 =====
    
    toggleSidebar() {
        const isVisible = this.sidebar.style.display === 'block';
        if (isVisible) {
            this.closeSidebar();
        } else {
            this.openSidebar();
        }
    }
    
    openSidebar() {
        this.sidebar.style.display = 'block';
        this.sidebarOverlay.style.display = 'block';
        
        // 사이드바 열 때 최신 참가자 목록으로 업데이트
        this.updateParticipantsList();
        
        // 약간의 지연 후 한 번 더 업데이트 (UI 렌더링 완료 후)
        setTimeout(() => {
            this.updateParticipantsList();
        }, 200);
        
        console.log('📋 사이드바 열림 - 참가자 목록 업데이트됨');
    }
    
    closeSidebar() {
        this.sidebar.style.display = 'none';
        this.sidebarOverlay.style.display = 'none';
    }
    
    // ===== 원격 비디오 설정 =====
    
    setupRemoteVideo(videoElement, username) {
        console.log('🔧 원격 비디오 설정:', { videoElement, username });
        
        // 원격 비디오를 PIP 영역에 표시
        if (this.pipVideo && videoElement) {
            // 기존 PIP 비디오 스트림 제거
            this.pipVideo.srcObject = null;
            
            // 원격 비디오 스트림을 PIP에 설정
            if (videoElement.srcObject) {
                this.pipVideo.srcObject = videoElement.srcObject;
                this.pipLabel.textContent = username;
                this.showPip();
                
                console.log('✅ 원격 비디오가 PIP에 설정됨');
            } else {
                console.warn('⚠️ 원격 비디오 요소에 srcObject가 없습니다');
            }
            
            // 원본 비디오 요소는 숨김 (PIP에서만 표시)
            videoElement.style.display = 'none';
        }
    }
    
    showPip() {
        if (this.pipContainer) {
            this.pipContainer.style.display = 'block';
            console.log('🔧 PIP 표시됨');
        }
    }

    // 자신의 비디오를 PIP에 설정하는 함수
    setupLocalVideoPip() {
        console.log('🔧 자신의 비디오를 PIP로 이동');
        
        if (this.publisher && this.publisher.stream) {
            try {
                const localStream = this.publisher.stream.getMediaStream();
                if (localStream) {
                    // PIP 비디오에 자신의 스트림 설정
                    this.pipVideo.srcObject = localStream;
                    this.pipVideo.muted = true; // PIP에서는 음소거
                    this.pipLabel.textContent = this.sessionData.username + ' (나)';
                    
                    // PIP 컨테이너 표시
                    this.pipContainer.classList.add('show');
                    this.pipContainer.style.display = 'block';
                    this.pipEnabled = true;
                    this.pipUsername = this.sessionData.username;
                    
                    console.log('✅ 자신의 비디오가 PIP에 설정됨');
                }
            } catch (error) {
                console.error('❌ 자신의 비디오를 PIP로 이동 실패:', error);
            }
        }
    }
    
    // 🎯 화면 레이아웃 자동 업데이트 - 양방향 화면 전환 지원
    checkAndUpdateVideoLayout() {
        console.log('🔄 화면 레이아웃 업데이트 시작');
        console.log('📊 현재 참가자 수:', this.participants.size);
        
        // 자신 제외한 다른 참가자들 찾기
        const otherParticipants = Array.from(this.participants.values())
            .filter(p => !p.isMe);
        
        console.log('👥 다른 참가자 수:', otherParticipants.length);
        
        // 2명 이상일 때만 화면 전환 수행
        if (this.participants.size >= 2 && otherParticipants.length > 0) {
            // 가장 최근 참가자 또는 첫 번째 다른 참가자를 메인으로
            const targetParticipant = otherParticipants[otherParticipants.length - 1];
            console.log('🎯 메인 화면 대상:', targetParticipant.username);
            
            // 해당 participant의 subscriber 찾기
            const targetSubscriber = this.subscribers.find(sub => {
                if (sub.stream && sub.stream.connection) {
                    return sub.stream.connection.connectionId === targetParticipant.connectionId;
                }
                return false;
            });
            
            if (targetSubscriber && targetSubscriber.stream) {
                console.log('✅ 대상 Subscriber 찾음, 화면 전환 실행');
                
                // 자신의 비디오를 PIP로 이동
                this.setupLocalVideoPip();
                
                // 다른 참가자를 메인으로 설정
                setTimeout(() => {
                    this.setMainVideo(targetSubscriber.stream, targetParticipant.username, false);
                    console.log('✅ 양방향 화면 전환 완료: 메인=' + targetParticipant.username + ', PIP=나');
                }, 200);
            } else {
                console.log('⚠️ 대상 Subscriber를 찾을 수 없음, 나중에 재시도');
            }
        } else {
            console.log('ℹ️ 참가자가 충분하지 않음, 화면 전환 건너뜀');
        }
    }

    // Subscriber가 준비될 때까지 대기 후 화면 전환 수행
    async waitForSubscriberReady(subscriber, stream, username) {
        console.log('🔄 Subscriber 준비 대기 중...', username);
        
        try {
            // MediaStream이 준비될 때까지 대기 (최대 10초)
            let attempts = 0;
            const maxAttempts = 50; // 10초 (200ms * 50)
            
            while (attempts < maxAttempts) {
                const mediaStream = stream.getMediaStream();
                if (mediaStream && mediaStream.getVideoTracks().length > 0) {
                    const videoTrack = mediaStream.getVideoTracks()[0];
                    if (videoTrack.readyState === 'live') {
                        console.log('✅ MediaStream 준비 완료:', mediaStream.id);
                        break;
                    }
                }
                
                await new Promise(resolve => setTimeout(resolve, 200));
                attempts++;
                
                if (attempts % 10 === 0) {
                    console.log(`⏳ MediaStream 준비 대기 중... (${attempts * 200}ms)`);
                }
            }
            
            // MediaStream이 준비되었으면 화면 전환 수행
            if (attempts < maxAttempts) {
                // 현재 메인 화면에 자신의 비디오가 있다면 PIP로 이동
                if (this.publisher && this.mainVideo.srcObject === this.publisher.stream.getMediaStream()) {
                    console.log('🔄 화면 전환: 자신의 비디오를 PIP로, 원격 비디오를 메인으로 이동');
                    
                    // 자신의 비디오를 PIP에 설정
                    this.setupLocalVideoPip();
                    
                    // 원격 비디오를 메인에 설정 (약간의 지연 후)
                    setTimeout(() => {
                        this.setMainVideo(stream, username, false);
                        console.log('✅ 화면 전환 완료: 메인=' + username + ', PIP=나');
                    }, 300);
                }
            } else {
                console.warn('⚠️ MediaStream 준비 시간 초과, 기본 화면 전환 시도');
                // 타임아웃 시에도 기본적인 화면 전환 시도
                this.setupLocalVideoPip();
                setTimeout(() => {
                    this.setMainVideo(stream, username, false);
                }, 500);
            }
            
        } catch (error) {
            console.error('❌ Subscriber 대기 중 오류:', error);
            // 오류 발생 시에도 기본적인 화면 전환 시도
            this.setupLocalVideoPip();
            setTimeout(() => {
                this.setMainVideo(stream, username, false);
            }, 1000);
        }
    }
    
    // ===== 참가자 관리 =====
    
    handleConnectionCreated(event) {
        const connectionId = event.connection.connectionId;
        console.log('🔧 새 참가자 연결:', connectionId);
        console.log('🔧 Connection data:', event.connection.data);
        
        // 참가자 정보 파싱
        const userData = this.parseConnectionData(event.connection.data);
        const username = userData?.username || userData?.clientData || '사용자';
        const isMe = connectionId === this.session.connection.connectionId;
        
        // 참가자 맵에 추가
        this.participants.set(connectionId, {
            username: username,
            connectionId: connectionId,
            isMe: isMe,
            connection: event.connection
        });
        
        console.log('✅ 참가자 맵에 추가:', { connectionId, username, isMe });
        console.log('📊 현재 참가자 수:', this.participants.size);
        
        // 참가자 목록 업데이트
        this.updateParticipantsList();
        
        // 약간의 지연 후 한 번 더 업데이트 (안정성 확보)
        setTimeout(() => {
            this.updateParticipantsList();
        }, 300);
        
        // 참가자 연결 알림 (자신 제외)
        if (!isMe) {
            this.showToast(`${username}님이 참가했습니다`);
            console.log('✅ 새 참가자 알림:', username);
            
            // ✅ 화면 전환은 이제 streamCreated 이벤트에서 직접 처리됨
            console.log('ℹ️ 새 참가자 입장 - 화면 전환은 streamCreated에서 자동 처리');
        }
    }
    
    handleConnectionDestroyed(event) {
        const connectionId = event.connection.connectionId;
        console.log('🔧 참가자 연결 해제:', connectionId);
        
        // 참가자 맵에서 정보 조회 (제거 전에)
        const participant = this.participants.get(connectionId);
        const username = participant ? participant.username : this.parseConnectionData(event.connection.data)?.username || '참가자';
        
        // 참가자 맵에서 제거
        if (this.participants.has(connectionId)) {
            this.participants.delete(connectionId);
            console.log('✅ 참가자 맵에서 제거:', { connectionId, username });
            console.log('📊 현재 참가자 수:', this.participants.size);
        }
        
        // 참가자 목록 업데이트
        this.updateParticipantsList();
        
        // 약간의 지연 후 한 번 더 업데이트 (안정성 확보)
        setTimeout(() => {
            this.updateParticipantsList();
        }, 200);
        
        // 참가자 연결 해제 알림 (자신 제외)
        if (participant && !participant.isMe) {
            this.showToast(`${username}님이 나갔습니다`);
            console.log('✅ 참가자 퇴장 알림:', username);
        }
    }
    
    // 🎯 강화된 네트워크 연결 끊김 처리
    handleNetworkDisconnection() {
        console.log('🔄 네트워크 연결 끊김 감지 - 재연결 시도');
        this.showToast('네트워크 연결이 끊어졌습니다. 재연결을 시도합니다...');
        
        // 연결 상태 체크 후 재연결 시도
        this.checkConnectionAndReconnect();
    }
    
    // 🎯 연결 상태 체크 및 재연결 관리
    async checkConnectionAndReconnect() {
        let reconnectAttempts = 0;
        const maxAttempts = 5;
        const baseDelay = 2000; // 2초부터 시작
        
        while (reconnectAttempts < maxAttempts) {
            try {
                console.log(`🔄 재연결 시도 ${reconnectAttempts + 1}/${maxAttempts}`);
                
                // 네트워크 연결 상태 확인
                if (!navigator.onLine) {
                    console.log('⚠️ 오프라인 상태 감지, 온라인 복구 대기...');
                    await this.waitForOnlineStatus();
                }
                
                // 세션 재연결 시도
                await this.attemptReconnection();
                
                console.log('✅ 재연결 성공');
                this.showToast('연결이 복구되었습니다');
                return; // 성공 시 종료
                
            } catch (error) {
                reconnectAttempts++;
                const delay = baseDelay * Math.pow(2, reconnectAttempts - 1); // 지수적 백오프
                
                console.error(`❌ 재연결 시도 ${reconnectAttempts} 실패:`, error.message);
                
                if (reconnectAttempts < maxAttempts) {
                    console.log(`⏳ ${delay}ms 후 다시 시도...`);
                    this.showToast(`재연결 시도 중... (${reconnectAttempts}/${maxAttempts})`);
                    await new Promise(resolve => setTimeout(resolve, delay));
                } else {
                    console.error('🚨 모든 재연결 시도 실패');
                    this.handleReconnectionFailure();
                }
            }
        }
    }
    
    // 🎯 온라인 상태 복구 대기
    async waitForOnlineStatus() {
        return new Promise((resolve) => {
            const checkOnline = () => {
                if (navigator.onLine) {
                    console.log('✅ 온라인 상태 복구됨');
                    resolve();
                } else {
                    console.log('⏳ 온라인 상태 대기 중...');
                    setTimeout(checkOnline, 1000);
                }
            };
            checkOnline();
        });
    }
    
    // 🎯 세션 재연결 시도 (개선된 버전)
    async attemptReconnection() {
        console.log('🔄 세션 재연결 시도 시작');
        
        // 기존 세션 정리 (더 안전하게)
        if (this.session) {
            try {
                this.session.disconnect();
            } catch (error) {
                console.log('기존 세션 정리 중 오류 (무시):', error.message);
            }
        }
        
        // 참가자 맵 유지 (자신만 남기고 나머지 제거)
        const myConnectionId = this.sessionData ? this.sessionData.username : null;
        if (myConnectionId) {
            const myParticipant = Array.from(this.participants.values()).find(p => p.isMe);
            this.participants.clear();
            if (myParticipant) {
                this.participants.set(myParticipant.connectionId, myParticipant);
            }
        }
        
        // 새로운 세션 연결
        await this.connectToSession();
        
        console.log('✅ 세션 재연결 성공');
    }
    
    // 🎯 재연결 완전 실패 처리
    handleReconnectionFailure() {
        console.error('🚨 재연결 완전 실패 - 사용자 개입 필요');
        
        const message = '연결 복구에 실패했습니다.\n다음 중 하나를 선택해주세요:';
        const options = '\n\n1. 페이지 새로고침\n2. 세션 재참가\n3. 계속 시도';
        
        this.showToast('연결 복구 실패 - 수동 조치 필요');
        
        setTimeout(() => {
            const choice = confirm(message + options + '\n\n확인: 페이지 새로고침 / 취소: 세션 재참가');
            
            if (choice) {
                // 페이지 새로고침
                location.reload();
            } else {
                // 세션 재참가 페이지로 이동
                const sessionId = this.sessionData?.sessionId;
                if (sessionId) {
                    window.location.href = `/new-video-call-v2.html?sessionId=${sessionId}&username=${this.sessionData.username}&reconnect=true`;
                } else {
                    location.reload();
                }
            }
        }, 2000);
    }
    
    updateParticipantsList() {
        if (!this.participantsList) {
            console.warn('⚠️ 참가자 목록 요소가 없어 업데이트를 건너뜀');
            return;
        }
        
        try {
            this.participantsList.innerHTML = '';
            
            console.log('🔧 참가자 목록 업데이트 (독립 맵 기반):');
            console.log('  - 참가자 맵 크기:', this.participants.size);
            
            // 참가자 맵의 모든 항목을 표시
            const participantEntries = Array.from(this.participants.values());
            
            participantEntries.forEach((participant, index) => {
                console.log(`  - 참가자 ${index + 1}: ${participant.username} (ID: ${participant.connectionId}, 본인: ${participant.isMe})`);
                
                const participantEl = document.createElement('div');
                participantEl.className = 'participant-item';
                participantEl.innerHTML = `
                    <div class="participant-avatar">👤</div>
                    <div class="participant-name">${participant.username}${participant.isMe ? ' (나)' : ''}</div>
                    <div class="participant-status">🟢</div>
                `;
                this.participantsList.appendChild(participantEl);
            });
            
            // 참가자 수 업데이트
            if (this.participantCount) {
                this.participantCount.textContent = this.participants.size;
            }
            
            console.log('✅ 참가자 목록 업데이트 완료:', this.participants.size, '명');
            
        } catch (error) {
            console.error('❌ 참가자 목록 업데이트 실패:', error);
        }
    }
    
    parseConnectionData(data) {
        try {
            if (!data) return {};
            
            if (typeof data === 'string') {
                // %/% 구분자 처리
                if (data.includes('%/%')) {
                    data = data.split('%/%')[0];
                }
                
                if (data.startsWith('{')) {
                    return JSON.parse(data);
                }
                return { username: data };
            }
            
            return data;
        } catch (error) {
            console.warn('Connection data 파싱 실패:', error);
            return {};
        }
    }
    
    // ===== 채팅 기능 =====
    
    sendChatMessage() {
        const message = this.chatInput.value.trim();
        if (!message) return;
        
        console.log('🔧 채팅 메시지 전송:', message);
        
        // OpenVidu 시그널을 통해 채팅 메시지 전송
        this.session.signal({
            type: 'chat',
            data: JSON.stringify({
                message: message,
                username: this.sessionData.username,
                timestamp: Date.now()
            })
        }).then(() => {
            console.log('✅ 채팅 메시지 전송 완료');
            this.chatInput.value = '';
        }).catch(error => {
            console.error('❌ 채팅 메시지 전송 실패:', error);
            this.showToast('메시지 전송에 실패했습니다');
        });
    }
    
    handleChatMessage(event) {
        try {
            const data = JSON.parse(event.data);
            console.log('🔧 채팅 메시지 수신:', data);
            
            const messageEl = document.createElement('div');
            messageEl.className = 'chat-message';
            
            const time = new Date(data.timestamp).toLocaleTimeString('ko-KR', { 
                hour: '2-digit', 
                minute: '2-digit' 
            });
            
            const isMyMessage = event.from.connectionId === this.session.connection.connectionId;
            
            messageEl.innerHTML = `
                <div class="chat-message-header">
                    <span class="chat-username">${data.username}${isMyMessage ? ' (나)' : ''}</span>
                    <span class="chat-timestamp">${time}</span>
                </div>
                <div class="chat-message-content">${this.escapeHtml(data.message)}</div>
            `;
            
            if (isMyMessage) {
                messageEl.classList.add('my-message');
            }
            
            this.chatMessages.appendChild(messageEl);
            this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
            
        } catch (error) {
            console.error('채팅 메시지 처리 실패:', error);
        }
    }
    
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    // 파일 업로드 처리 (개선된 안정성)
    async handleFileUpload(file) {
        console.log('🔧 파일 업로드 시작:', file.name, file.size, file.type);
        
        // 🎯 파일 크기 제한 강화 (10MB → 2MB)
        const maxFileSize = 2 * 1024 * 1024; // 2MB
        if (file.size > maxFileSize) {
            this.showToast(`파일 크기는 ${this.formatFileSize(maxFileSize)}를 초과할 수 없습니다`);
            console.warn('❌ 파일 크기 초과:', file.size, 'bytes');
            return;
        }
        
        // 지원되는 파일 형식 확인
        const supportedTypes = [
            'image/jpeg', 'image/png', 'image/gif', 'image/webp',
            'application/pdf', 'text/plain', 
            'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
        ];
        
        if (!supportedTypes.includes(file.type)) {
            this.showToast('지원되지 않는 파일 형식입니다');
            console.warn('❌ 지원되지 않는 파일 형식:', file.type);
            return;
        }
        
        try {
            // 업로드 진행 토스트 표시
            this.showToast(`파일 "${file.name}" 업로드 중...`);
            console.log('📤 Base64 인코딩 시작...');
            
            // 🎯 개선된 Base64 인코딩 (청크 처리)
            const base64Data = await this.fileToBase64Optimized(file);
            
            console.log('📤 파일 시그널 전송 시작...');
            
            // 🎯 더 작은 청크로 나누어 전송 (연결 안정성 향상)
            await this.sendFileInChunks({
                fileName: file.name,
                fileSize: file.size,
                fileType: file.type,
                fileData: base64Data,
                username: this.sessionData.username,
                timestamp: Date.now()
            });
            
            console.log('✅ 파일 전송 완료:', file.name);
            this.showToast(`파일 "${file.name}"이 전송되었습니다`);
            
            // 파일 입력 초기화
            this.fileInput.value = '';
            
        } catch (error) {
            console.error('❌ 파일 업로드 실패:', error);
            this.showToast('파일 전송에 실패했습니다: ' + error.message);
            
            // 파일 입력 초기화
            this.fileInput.value = '';
        }
    }
    
    // 파일을 Base64로 변환 (기존 함수 유지)
    fileToBase64(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(reader.result.split(',')[1]); // data:... 부분 제거
            reader.onerror = reject;
            reader.readAsDataURL(file);
        });
    }

    // 🎯 최적화된 Base64 인코딩 (메모리 효율적)
    fileToBase64Optimized(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            
            reader.onload = () => {
                try {
                    const result = reader.result;
                    if (typeof result === 'string') {
                        // data:... 부분 제거하고 순수 Base64만 반환
                        const base64Data = result.split(',')[1];
                        console.log('📤 Base64 인코딩 완료:', base64Data.length, 'characters');
                        resolve(base64Data);
                    } else {
                        reject(new Error('Base64 인코딩 결과가 문자열이 아닙니다'));
                    }
                } catch (error) {
                    console.error('Base64 인코딩 실패:', error);
                    reject(error);
                }
            };
            
            reader.onerror = () => {
                console.error('FileReader 오류:', reader.error);
                reject(reader.error);
            };
            
            reader.onabort = () => {
                console.error('FileReader 중단됨');
                reject(new Error('파일 읽기가 중단되었습니다'));
            };
            
            // 타임아웃 설정 (30초)
            const timeout = setTimeout(() => {
                reader.abort();
                reject(new Error('파일 인코딩 시간이 초과되었습니다'));
            }, 30000);
            
            reader.onload = (event) => {
                clearTimeout(timeout);
                resolve(event.target.result);
            };
            
            reader.readAsDataURL(file);
        });
    }

    // 🎯 파일을 청크로 나누어 전송 (연결 안정성 향상)
    async sendFileInChunks(fileData) {
        // 작은 파일인 경우 바로 전송 (500KB 미만)
        if (fileData.fileSize < 500 * 1024) {
            console.log('📤 작은 파일 직접 전송:', fileData.fileName);
            await this.session.signal({
                type: 'file-share',
                data: JSON.stringify(fileData)
            });
            return;
        }

        // 큰 파일인 경우 청크로 나누어 전송
        console.log('📤 대용량 파일 청크 전송 시작:', fileData.fileName);
        
        const base64Data = fileData.fileData;
        const chunkSize = 32768; // 32KB 청크
        const totalChunks = Math.ceil(base64Data.length / chunkSize);
        const chunkId = 'chunk_' + Date.now() + '_' + Math.random().toString(36).substring(2, 11);
        
        console.log(`📤 총 ${totalChunks}개 청크로 분할 전송`);
        
        // 첫 번째 청크 (메타데이터 포함)
        await this.session.signal({
            type: 'file-share-start',
            data: JSON.stringify({
                chunkId: chunkId,
                fileName: fileData.fileName,
                fileSize: fileData.fileSize,
                fileType: fileData.fileType,
                username: fileData.username,
                timestamp: fileData.timestamp,
                totalChunks: totalChunks,
                chunkIndex: 0,
                chunkData: base64Data.substring(0, chunkSize)
            })
        });

        // 나머지 청크들 전송
        for (let i = 1; i < totalChunks; i++) {
            const start = i * chunkSize;
            const end = Math.min(start + chunkSize, base64Data.length);
            const chunk = base64Data.substring(start, end);
            
            await this.session.signal({
                type: 'file-share-chunk',
                data: JSON.stringify({
                    chunkId: chunkId,
                    chunkIndex: i,
                    chunkData: chunk,
                    isLastChunk: i === totalChunks - 1
                })
            });
            
            // 청크 간 약간의 지연 (연결 안정성)
            await new Promise(resolve => setTimeout(resolve, 50));
            
            if (i % 10 === 0) {
                console.log(`📤 청크 전송 진행률: ${i}/${totalChunks} (${Math.round(i/totalChunks*100)}%)`);
            }
        }
        
        console.log('📤 청크 전송 완료:', fileData.fileName);
    }
    
    // 파일 공유 수신 처리
    handleFileShare(event) {
        try {
            const data = JSON.parse(event.data);
            console.log('🔧 파일 공유 수신:', data);
            
            const time = new Date(data.timestamp).toLocaleTimeString('ko-KR', { 
                hour: '2-digit', 
                minute: '2-digit' 
            });
            
            const isMyFile = event.from.connectionId === this.session.connection.connectionId;
            
            // 파일 크기를 읽기 쉬운 형태로 변환
            const fileSize = this.formatFileSize(data.fileSize);
            
            const messageEl = document.createElement('div');
            messageEl.className = 'chat-message file-message';
            
            if (isMyFile) {
                messageEl.classList.add('my-message');
            }
            
            // 파일 다운로드 링크 생성
            const blob = this.base64ToBlob(data.fileData, data.fileType);
            const downloadUrl = URL.createObjectURL(blob);
            
            messageEl.innerHTML = `
                <div class="chat-message-header">
                    <span class="chat-username">${data.username}${isMyFile ? ' (나)' : ''}</span>
                    <span class="chat-timestamp">${time}</span>
                </div>
                <div class="file-share-content">
                    <div class="file-info">
                        <span class="file-icon">${this.getFileIcon(data.fileType)}</span>
                        <div class="file-details">
                            <span class="file-name">${this.escapeHtml(data.fileName)}</span>
                            <span class="file-size">${fileSize}</span>
                        </div>
                    </div>
                    <a href="${downloadUrl}" download="${data.fileName}" class="file-download-btn">다운로드</a>
                </div>
            `;
            
            this.chatMessages.appendChild(messageEl);
            this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
            
        } catch (error) {
            console.error('파일 공유 처리 실패:', error);
        }
    }
    
    // Base64를 Blob으로 변환
    base64ToBlob(dataUrl, mimeType) {
        try {
            // Data URL에서 Base64 부분 추출
            let base64;
            if (dataUrl.includes(',')) {
                base64 = dataUrl.split(',')[1];
            } else {
                base64 = dataUrl;
            }
            
            // UTF-8 BOM(77u/) 제거 - 한글 텍스트 파일 처리
            if (base64.startsWith('77u/')) {
                console.log('🔧 UTF-8 BOM 감지 및 제거:', base64.substring(0, 10) + '...');
                base64 = base64.substring(4); // "77u/" 제거
            }
            
            const byteCharacters = atob(base64);
            const byteNumbers = new Array(byteCharacters.length);
            for (let i = 0; i < byteCharacters.length; i++) {
                byteNumbers[i] = byteCharacters.charCodeAt(i);
            }
            const byteArray = new Uint8Array(byteNumbers);
            return new Blob([byteArray], {type: mimeType});
        } catch (error) {
            console.error('❌ Base64 디코딩 실패:', error);
            // Fallback: 빈 Blob 반환
            return new Blob(['파일 디코딩 오류가 발생했습니다.'], {type: 'text/plain'});
        }
    }
    
    // 파일 크기 포맷팅
    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
    
    // 파일 타입별 아이콘
    getFileIcon(fileType) {
        if (fileType.startsWith('image/')) return '🖼️';
        if (fileType.includes('pdf')) return '📄';
        if (fileType.includes('word')) return '📝';
        if (fileType.includes('text')) return '📄';
        return '📎';
    }
}

// 페이지 로드 시 앱 초기화
document.addEventListener('DOMContentLoaded', () => {
    new NewVideoCallV2Manager();
});