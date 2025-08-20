// OpenVidu 화상통화 ver2 JavaScript

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
        } catch (error) {
            console.error('앱 초기화 실패:', error);
            this.showToast('연결에 실패했습니다: ' + error.message);
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
        this.sidebarBtn.addEventListener('click', () => this.openSidebar());
        
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
    
    configureOpenVidu() {
        // OpenVidu 서버 URL 전역 설정 강제 변경
        window.OPENVIDU_SERVER_URL = 'http://localhost:25565';
        
        // WebSocket 생성을 가로채서 URL 강제 변경
        const originalWebSocket = window.WebSocket;
        window.WebSocket = function(url, protocols) {
            console.log('Original WebSocket URL:', url);
            
            // 모든 4443 포트를 25565로 변경
            if (url.includes(':4443')) {
                url = url.replace(':4443', ':25565');
                console.log('Modified WebSocket URL:', url);
            }
            
            return new originalWebSocket(url, protocols);
        };
        
        // 프로토타입 상속 복원
        Object.setPrototypeOf(window.WebSocket, originalWebSocket);
        Object.defineProperty(window.WebSocket, 'prototype', {
            value: originalWebSocket.prototype,
            writable: false
        });
        
        // HTTPS 환경에서도 작동하도록 설정
        if (typeof navigator !== 'undefined' && navigator.userAgent) {
            console.log('User Agent:', navigator.userAgent);
        }
    }
    
    async connectToSession() {
        this.showLoading(true);
        
        try {
            // OpenVidu 객체 생성 전 환경 확인
            if (typeof OpenVidu === 'undefined') {
                throw new Error('OpenVidu 라이브러리가 로드되지 않았습니다.');
            }
            
            // WebRTC 지원 확인
            if (!window.RTCPeerConnection) {
                throw new Error('이 브라우저는 WebRTC를 지원하지 않습니다.');
            }
            
            // OpenVidu 객체 생성
            this.OV = new OpenVidu();
            
            // 서버 URL 설정 (강제)
            this.OV.setAdvancedConfiguration({
                iceServers: [{
                    urls: 'turn:localhost:3478',
                    username: '1755753031:DvbMFsw3',
                    credential: 'Nw5EfhmWE5Mg5kmuW/eY6lEnqrg='
                }]
            });
            
            this.session = this.OV.initSession();
            
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
            });
            
            this.session.on('connectionDestroyed', (event) => {
                console.log('연결 파괴됨:', event);
            });
            
            // 세션에 연결 (토큰 URL 수정)
            let connectToken = this.sessionData.token;
            
            // 토큰 URL의 포트도 25565로 변경
            if (connectToken && connectToken.includes(':4443')) {
                connectToken = connectToken.replace(':4443', ':25565');
                console.log('Modified token URL:', connectToken);
            }
            
            await this.session.connect(connectToken, { clientData: this.sessionData.username });
            
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
            
            console.log('로컬 스트림 초기화 완료');
            
        } catch (error) {
            console.error('로컬 스트림 초기화 실패:', error);
            throw error;
        }
    }
    
    handleStreamCreated(event) {
        console.log('새 스트림 수신:', event.stream.streamId);
        
        // Subscriber 생성
        const subscriber = this.session.subscribe(event.stream, undefined);
        this.subscribers.push(subscriber);
        
        // 원격 스트림을 메인 비디오에 표시
        const connection = event.stream.connection;
        let username = '사용자';
        
        try {
            // connection.data 파싱 시도
            if (connection.data) {
                console.log('Connection data:', connection.data);
                
                // 여러 형태의 데이터 형식 지원
                if (typeof connection.data === 'string') {
                    // JSON 문자열인 경우
                    if (connection.data.startsWith('{')) {
                        const parsedData = JSON.parse(connection.data);
                        username = parsedData.clientData || parsedData.username || '사용자';
                    } else {
                        // 단순 문자열인 경우
                        username = connection.data;
                    }
                } else if (typeof connection.data === 'object') {
                    // 이미 객체인 경우
                    username = connection.data.clientData || connection.data.username || '사용자';
                }
            }
        } catch (error) {
            console.warn('Connection data 파싱 실패:', error);
            console.log('Raw connection data:', connection.data);
            // 기본값 사용
            username = '사용자' + Math.floor(Math.random() * 1000);
        }
        
        // 현재 화면공유 중이 아니면 메인 비디오 변경
        if (!this.isScreenSharing) {
            this.setMainVideo(event.stream, username, false);
        }
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
    
    setMainVideo(stream, username, isScreenShare = false) {
        try {
            if (stream && stream.getMediaStream) {
                const mediaStream = stream.getMediaStream();
                this.mainVideo.srcObject = mediaStream;
                this.mainVideoLabel.textContent = isScreenShare ? username + ' (화면공유)' : username;
                
                // 비디오 트랙 확인
                const videoTracks = mediaStream.getVideoTracks();
                if (videoTracks.length > 0 && videoTracks[0].readyState === 'live') {
                    this.mainVideoOverlay.classList.add('hidden');
                } else {
                    this.mainVideoOverlay.classList.remove('hidden');
                }
            }
        } catch (error) {
            console.error('메인 비디오 설정 실패:', error);
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
            
            // 화면공유 Publisher 생성
            this.screenSharePublisher = await this.OV.initPublisherAsync(undefined, {
                videoSource: screenStream.getVideoTracks()[0],
                audioSource: screenStream.getAudioTracks()[0] || false,
                publishVideo: true,
                publishAudio: true,
                mirror: false
            });
            
            // 기존 Publisher 언퍼블리시
            if (this.publisher) {
                await this.session.unpublish(this.publisher);
            }
            
            // 화면공유 Publisher 퍼블리시
            await this.session.publish(this.screenSharePublisher);
            
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
                    this.sendChatMessage(data.message);
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
    
    // 채팅 메시지 전송 (OpenVidu Signal 사용)
    async sendChatMessage(message) {
        try {
            await this.session.signal({
                data: JSON.stringify({
                    username: this.sessionData.username,
                    message: message
                }),
                type: 'chat'
            });
            
            // 채팅 저장 API 호출
            await this.saveChatMessage(message);
            
        } catch (error) {
            console.error('채팅 메시지 전송 실패:', error);
        }
    }
    
    // 채팅 메시지 데이터베이스 저장
    async saveChatMessage(message) {
        try {
            await fetch('/api/chat/messages', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    sessionId: this.sessionData.sessionId,
                    username: this.sessionData.username,
                    message: message
                })
            });
        } catch (error) {
            console.error('채팅 메시지 저장 실패:', error);
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
}

// 페이지 로드 시 앱 초기화
document.addEventListener('DOMContentLoaded', () => {
    new NewVideoCallV2Manager();
});