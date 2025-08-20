// 새로운 화상통화 페이지 JavaScript

class NewVideoCallManager {
    constructor() {
        // OpenVidu 객체 초기화 (올바른 URL로)
        this.ov = new OpenVidu();
        
        // OpenVidu 관련
        this.session = null;
        this.publisher = null;
        this.subscribers = [];
        this.sessionData = null;
        
        // 미디어 상태
        this.localVideoEnabled = true;
        this.localAudioEnabled = true;
        this.isScreenSharing = false;
        this.screenSharePublisher = null;
        
        // UI 상태
        this.callStartTime = null;
        this.callDurationTimer = null;
        this.currentMainVideo = null; // 현재 메인 화면에 표시되는 비디오
        
        // Picture-in-Picture 상태
        this.pipEnabled = false;
        this.pipStream = null;
        this.pipUsername = null;
        this.isDragging = false;
        this.dragOffset = { x: 0, y: 0 };
        
        // 참가자 관리
        this.participants = new Map(); // connectionId -> participant info
        
        this.initializeElements();
        this.loadSessionData();
        this.attachEventListeners();
        this.configureOpenVidu();
        this.initializeSession();
    }

    initializeElements() {
        // 헤더 요소들
        this.callDurationEl = document.getElementById('callDuration');
        this.currentUserNameEl = document.getElementById('currentUserName');
        this.currentUserAvatarEl = document.getElementById('currentUserAvatar');
        this.leaveSessionBtn = document.getElementById('leaveSessionBtn');
        
        // 메인 비디오 요소들
        this.mainVideo = document.getElementById('mainVideo');
        this.mainVideoOverlay = document.getElementById('mainVideoOverlay');
        this.mainVideoLabel = document.getElementById('mainVideoLabel');
        
        // 컨트롤 버튼들
        this.toggleAudioBtn = document.getElementById('toggleAudioBtn');
        this.toggleVideoBtn = document.getElementById('toggleVideoBtn');
        this.toggleScreenShareBtn = document.getElementById('toggleScreenShareBtn');
        
        // 사이드바 요소들
        this.participantsList = document.getElementById('participantsList');
        this.chatMessages = document.getElementById('chatMessages');
        this.chatInput = document.getElementById('chatInput');
        this.sendChatBtn = document.getElementById('sendChatBtn');
        
        // Picture-in-Picture 요소들
        this.pipContainer = document.getElementById('pipContainer');
        this.pipVideo = document.getElementById('pipVideo');
        this.pipLabel = document.getElementById('pipLabel');
        this.pipSwapBtn = document.getElementById('pipSwapBtn');
        this.pipCloseBtn = document.getElementById('pipCloseBtn');
        
        // 기타 요소들
        this.loadingScreen = document.getElementById('loadingScreen');
        this.toast = document.getElementById('toast');
        this.toastMessage = document.getElementById('toastMessage');
    }

    loadSessionData() {
        // URL 파라미터에서 세션 데이터 로드
        const urlParams = new URLSearchParams(window.location.search);
        this.sessionData = {
            sessionId: urlParams.get('sessionId') || localStorage.getItem('sessionId'),
            token: urlParams.get('token') || localStorage.getItem('token'),
            username: urlParams.get('username') || localStorage.getItem('username') || '사용자'
        };

        if (!this.sessionData.sessionId || !this.sessionData.token) {
            this.showToast('세션 정보가 없습니다. 메인 페이지로 이동합니다.');
            setTimeout(() => {
                window.location.href = '/';
            }, 2000);
            return;
        }

        // 사용자 정보 표시
        this.currentUserNameEl.textContent = this.sessionData.username;
        this.currentUserAvatarEl.textContent = this.getUserAvatar(this.sessionData.username);
    }

    attachEventListeners() {
        // 컨트롤 버튼 이벤트
        this.toggleAudioBtn.addEventListener('click', () => this.toggleAudio());
        this.toggleVideoBtn.addEventListener('click', () => this.toggleVideo());
        this.toggleScreenShareBtn.addEventListener('click', () => this.toggleScreenShare());
        this.leaveSessionBtn.addEventListener('click', () => this.leaveSession());
        
        // 채팅 이벤트
        this.sendChatBtn.addEventListener('click', () => this.sendChatMessage());
        this.chatInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.sendChatMessage();
            }
        });
        
        // PIP 관련 이벤트
        this.pipSwapBtn.addEventListener('click', () => this.swapPipAndMain());
        this.pipCloseBtn.addEventListener('click', () => this.hidePip());
        this.pipContainer.addEventListener('mousedown', (e) => this.startDragging(e));
        this.pipContainer.addEventListener('dblclick', () => this.swapPipAndMain());
        
        // 터치 이벤트 지원
        this.pipContainer.addEventListener('touchstart', (e) => this.startDragging(e.touches[0]));
        this.pipContainer.addEventListener('touchmove', (e) => {
            e.preventDefault();
            this.handleDragging(e.touches[0]);
        });
        this.pipContainer.addEventListener('touchend', () => this.stopDragging());
        
        // 전역 마우스 이벤트 (드래깅용)
        document.addEventListener('mousemove', (e) => this.handleDragging(e));
        document.addEventListener('mouseup', () => this.stopDragging());
        document.addEventListener('touchmove', (e) => {
            if (this.isDragging) {
                e.preventDefault();
                this.handleDragging(e.touches[0]);
            }
        });
        document.addEventListener('touchend', () => this.stopDragging());
        
        // 키보드 단축키
        document.addEventListener('keydown', (e) => {
            if (e.ctrlKey && e.key === 'p') {
                e.preventDefault();
                this.togglePip();
            }
        });
        
        // 창 닫기 이벤트
        window.addEventListener('beforeunload', () => {
            if (this.session) {
                this.session.disconnect();
            }
        });
    }

    configureOpenVidu() {
        // OpenVidu 설정
        console.log('Configuring OpenVidu with URL:', window.OPENVIDU_SERVER_URL);
        
        // WebSocket URL을 올바른 포트로 설정
        const wsUrl = window.OPENVIDU_SERVER_URL.replace('http://', 'ws://') + '/openvidu';
        console.log('Setting WebSocket URL to:', wsUrl);
        
        // WebSocket 생성을 가로채서 URL 강제 변경
        const originalWebSocket = window.WebSocket;
        window.WebSocket = function(url, protocols) {
            console.log('Original WebSocket URL:', url);
            
            // 4443 포트를 25565로 강제 변경
            if (url.includes('localhost:4443')) {
                url = url.replace('localhost:4443', 'localhost:25565');
                console.log('Modified WebSocket URL:', url);
            }
            
            return new originalWebSocket(url, protocols);
        };
        
        // WebSocket 프로토타입 복사
        window.WebSocket.prototype = originalWebSocket.prototype;
        window.WebSocket.CONNECTING = originalWebSocket.CONNECTING;
        window.WebSocket.OPEN = originalWebSocket.OPEN;
        window.WebSocket.CLOSING = originalWebSocket.CLOSING;
        window.WebSocket.CLOSED = originalWebSocket.CLOSED;
        
        // OpenVidu 고급 설정
        this.ov.setAdvancedConfiguration({
            iceServers: [{
                urls: 'stun:stun.l.google.com:19302'
            }]
        });
        
        console.log('OpenVidu configuration completed with WebSocket interception');
    }

    async initializeSession() {
        try {
            this.showLoadingScreen(true);
            
            // OpenVidu 세션 초기화 (명시적으로 WebSocket URL 설정)
            this.session = this.ov.initSession();
            
            // 세션 이벤트 리스너 등록
            this.session.on('streamCreated', (event) => {
                this.handleStreamCreated(event);
            });
            
            this.session.on('streamDestroyed', (event) => {
                this.handleStreamDestroyed(event);
            });
            
            this.session.on('signal:chat', (event) => {
                this.handleChatMessage(event);
            });
            
            this.session.on('connectionCreated', (event) => {
                this.handleConnectionCreated(event);
            });
            
            this.session.on('connectionDestroyed', (event) => {
                this.handleConnectionDestroyed(event);
            });

            // 세션 연결
            await this.session.connect(this.sessionData.token, { 
                clientData: this.sessionData.username 
            });
            
            // 로컬 스트림 발행
            await this.initializeLocalStream();
            
            // 통화 시간 추적 시작
            this.startCallTimer();
            
            // 이전 채팅 메시지 로드
            await this.loadChatHistory();
            
            this.showLoadingScreen(false);
            this.showToast('화상통화에 연결되었습니다.');
            
        } catch (error) {
            console.error('세션 초기화 실패:', error);
            this.showToast('화상통화 연결에 실패했습니다.');
            this.showLoadingScreen(false);
        }
    }

    async initializeLocalStream() {
        try {
            const publisher = await this.ov.initPublisherAsync(undefined, {
                audioSource: undefined,
                videoSource: undefined,
                publishAudio: this.localAudioEnabled,
                publishVideo: this.localVideoEnabled,
                resolution: '640x480',
                frameRate: 30,
                insertMode: 'APPEND',
                mirror: false
            });
            
            this.publisher = publisher;
            await this.session.publish(this.publisher);
            
            // 로컬 비디오를 메인 화면에 표시
            this.setMainVideo(publisher.stream, this.sessionData.username, true);
            
            // 참가자 목록에 자신 추가
            this.addParticipant(this.session.connection.connectionId, this.sessionData.username, true);
            
        } catch (error) {
            console.error('로컬 스트림 초기화 실패:', error);
            this.showToast('카메라/마이크 접근에 실패했습니다.');
        }
    }

    handleStreamCreated(event) {
        const subscriber = this.session.subscribe(event.stream, undefined);
        this.subscribers.push(subscriber);
        
        const participantData = JSON.parse(event.stream.connection.data);
        const username = participantData.clientData || '참가자';
        
        // 새 참가자를 참가자 목록에 추가
        this.addParticipant(event.stream.connection.connectionId, username, false);
        
        // 첫 번째 원격 스트림이면 메인 화면에 표시
        if (this.subscribers.length === 1 && !this.isScreenSharing) {
            this.setMainVideo(event.stream, username, false);
        }
        
        this.showToast(`${username}님이 입장했습니다.`);
    }

    handleStreamDestroyed(event) {
        const index = this.subscribers.findIndex(sub => sub.stream === event.stream);
        if (index >= 0) {
            this.subscribers.splice(index, 1);
        }
        
        const participantData = JSON.parse(event.stream.connection.data);
        const username = participantData.clientData || '참가자';
        
        // 참가자 목록에서 제거
        this.removeParticipant(event.stream.connection.connectionId);
        
        // 메인 화면의 스트림이 제거되면 다른 스트림으로 변경
        if (this.currentMainVideo === event.stream) {
            this.switchToNextAvailableStream();
        }
        
        this.showToast(`${username}님이 퇴장했습니다.`);
    }

    handleConnectionCreated(event) {
        console.log('새 연결 생성:', event.connection.connectionId);
    }

    handleConnectionDestroyed(event) {
        console.log('연결 제거:', event.connection.connectionId);
    }

    handleChatMessage(event) {
        const data = JSON.parse(event.data);
        this.displayChatMessage(data.username, data.message, false);
    }

    setMainVideo(stream, username, isLocal = false) {
        this.currentMainVideo = stream;
        
        try {
            // OpenVidu v2.30.0에서 올바른 방법으로 비디오 여부 확인
            let hasVideo = false;
            let mediaStream = null;
            
            if (stream && stream.getMediaStream) {
                mediaStream = stream.getMediaStream();
                hasVideo = mediaStream && mediaStream.getVideoTracks().length > 0;
            } else if (stream && stream.videoActive !== undefined) {
                hasVideo = stream.videoActive;
                mediaStream = stream.getMediaStream ? stream.getMediaStream() : stream;
            } else if (stream && stream.hasVideo !== undefined) {
                hasVideo = stream.hasVideo;
                mediaStream = stream.getMediaStream ? stream.getMediaStream() : stream;
            }
            
            console.log('Setting main video:', { hasVideo, username, isLocal });
            
            if (hasVideo && mediaStream) {
                this.mainVideo.srcObject = mediaStream;
                this.mainVideoOverlay.style.display = 'none';
            } else {
                this.mainVideo.srcObject = null;
                this.mainVideoOverlay.style.display = 'flex';
            }
            
        } catch (error) {
            console.error('Error setting main video:', error);
            this.mainVideo.srcObject = null;
            this.mainVideoOverlay.style.display = 'flex';
        }
        
        this.mainVideoLabel.textContent = isLocal ? '나' : username;
    }

    switchToNextAvailableStream() {
        if (this.subscribers.length > 0) {
            // 첫 번째 구독자 스트림으로 변경
            const nextStream = this.subscribers[0].stream;
            const participantData = JSON.parse(nextStream.connection.data);
            const username = participantData.clientData || '참가자';
            this.setMainVideo(nextStream, username, false);
        } else if (this.publisher) {
            // 구독자가 없으면 자신의 스트림으로 변경
            this.setMainVideo(this.publisher.stream, this.sessionData.username, true);
        }
    }

    addParticipant(connectionId, username, isLocal = false) {
        const participant = {
            connectionId,
            username,
            isLocal,
            isConnected: true,
            isSpeaking: false,
            isScreenSharing: false
        };
        
        this.participants.set(connectionId, participant);
        this.updateParticipantsList();
    }

    removeParticipant(connectionId) {
        this.participants.delete(connectionId);
        this.updateParticipantsList();
    }

    updateParticipantsList() {
        this.participantsList.innerHTML = '';
        
        this.participants.forEach((participant) => {
            const participantEl = document.createElement('div');
            participantEl.className = 'participant-item';
            participantEl.innerHTML = `
                <div class="participant-avatar">${this.getUserAvatar(participant.username)}</div>
                <div class="participant-info">
                    <div class="participant-name">${participant.username}${participant.isLocal ? ' (나)' : ''}</div>
                    <div class="participant-status ${participant.isSpeaking ? 'speaking' : ''} ${participant.isScreenSharing ? 'screen-sharing' : ''}">
                        ${participant.isScreenSharing ? '화면공유 중' : (participant.isSpeaking ? '말하는 중' : '온라인')}
                    </div>
                </div>
            `;
            
            // 클릭 시 메인 화면으로 변경
            if (!participant.isLocal) {
                participantEl.style.cursor = 'pointer';
                participantEl.addEventListener('click', () => {
                    const subscriber = this.subscribers.find(sub => 
                        sub.stream.connection.connectionId === participant.connectionId
                    );
                    if (subscriber) {
                        this.setMainVideo(subscriber.stream, participant.username, false);
                    }
                });
            }
            
            this.participantsList.appendChild(participantEl);
        });
    }

    getUserAvatar(username) {
        // 간단한 아바타 생성 (이름의 첫 글자 또는 이모지)
        if (username && username.length > 0) {
            return username.charAt(0).toUpperCase();
        }
        return '👤';
    }

    async toggleAudio() {
        if (this.publisher) {
            this.localAudioEnabled = !this.localAudioEnabled;
            this.publisher.publishAudio(this.localAudioEnabled);
            
            // 버튼 상태 업데이트
            if (this.localAudioEnabled) {
                this.toggleAudioBtn.classList.remove('disabled');
                this.toggleAudioBtn.classList.add('active');
            } else {
                this.toggleAudioBtn.classList.remove('active');
                this.toggleAudioBtn.classList.add('disabled');
            }
        }
    }

    async toggleVideo() {
        if (this.publisher) {
            this.localVideoEnabled = !this.localVideoEnabled;
            this.publisher.publishVideo(this.localVideoEnabled);
            
            // 버튼 상태 업데이트
            if (this.localVideoEnabled) {
                this.toggleVideoBtn.classList.remove('disabled');
                this.toggleVideoBtn.classList.add('active');
            } else {
                this.toggleVideoBtn.classList.remove('active');
                this.toggleVideoBtn.classList.add('disabled');
            }
            
            // 메인 화면이 자신의 스트림이면 오버레이 표시/숨김
            if (this.currentMainVideo === this.publisher.stream) {
                this.setMainVideo(this.publisher.stream, this.sessionData.username, true);
            }
        }
    }

    async toggleScreenShare() {
        if (this.isScreenSharing) {
            await this.stopScreenShare();
        } else {
            await this.startScreenShare();
        }
    }

    async startScreenShare() {
        try {
            // 화면공유 스트림 생성
            const screenStream = await navigator.mediaDevices.getDisplayMedia({
                video: true,
                audio: true
            });
            
            // PIP용 새로운 캠 스트림 생성 (기존 publisher unpublish 전에)
            try {
                console.log('PIP용 캠 스트림 생성 시작...');
                const camStream = await navigator.mediaDevices.getUserMedia({
                    video: {
                        width: { ideal: 320 },
                        height: { ideal: 240 },
                        frameRate: { ideal: 15 }
                    },
                    audio: false  // PIP에서는 오디오 비활성화
                });
                
                console.log('PIP 캠 스트림 생성 성공:', camStream);
                console.log('캠 스트림 트랙 개수:', camStream.getTracks().length);
                
                // PIP에 직접 설정 (showPip 메소드 우회)
                this.pipVideo.srcObject = camStream;
                this.pipVideo.muted = true; // PIP는 항상 음소거
                this.pipLabel.textContent = this.sessionData.username;
                this.pipContainer.classList.add('show');
                this.pipEnabled = true;
                this.pipStream = camStream; // 직접 MediaStream 저장
                this.pipUsername = this.sessionData.username;
                
                console.log('PIP 표시 완료:', this.sessionData.username);
                
                // PIP 비디오가 재생 시작되는지 확인
                this.pipVideo.onloadedmetadata = () => {
                    console.log('PIP 비디오 메타데이터 로드됨');
                    this.pipVideo.play().catch(e => console.warn('PIP 자동 재생 실패:', e));
                };
                
            } catch (pipError) {
                console.error('PIP 캠 스트림 생성 실패:', pipError);
                this.showToast('PIP 캠 스트림 생성 실패: ' + pipError.message);
            }
            
            // 화면공유 퍼블리셔 생성 (Content Hint 설정 포함)
            const videoTrack = screenStream.getVideoTracks()[0];
            if (videoTrack && videoTrack.contentHint !== undefined) {
                videoTrack.contentHint = 'detail'; // 화면공유에 적합한 Content Hint
            }
            
            this.screenSharePublisher = await this.ov.initPublisherAsync(undefined, {
                videoSource: videoTrack,
                audioSource: screenStream.getAudioTracks().length > 0 ? screenStream.getAudioTracks()[0] : undefined,
                publishAudio: screenStream.getAudioTracks().length > 0,
                publishVideo: true,
                resolution: '1280x720',
                frameRate: 15,
                insertMode: 'APPEND',
                mirror: false
            });
            
            // 기존 퍼블리셔 중단
            if (this.publisher) {
                await this.session.unpublish(this.publisher);
            }
            
            await this.session.publish(this.screenSharePublisher);
            
            // 화면공유를 메인 화면에 표시
            this.setMainVideo(this.screenSharePublisher.stream, this.sessionData.username + ' (화면공유)', true);
            
            this.isScreenSharing = true;
            this.toggleScreenShareBtn.classList.add('screen-sharing');
            
            // 백엔드에 화면공유 상태 알림
            await this.notifyScreenShareStart();
            
            // 화면공유 중단 감지
            screenStream.getVideoTracks()[0].addEventListener('ended', () => {
                this.stopScreenShare();
            });
            
            this.showToast('화면공유를 시작했습니다.');
            
        } catch (error) {
            console.error('화면공유 시작 실패:', error);
            this.showToast('화면공유를 시작할 수 없습니다.');
        }
    }

    async stopScreenShare() {
        try {
            if (this.screenSharePublisher) {
                await this.session.unpublish(this.screenSharePublisher);
                this.screenSharePublisher = null;
            }
            
            // PIP 스트림 정리
            if (this.pipVideo.srcObject) {
                const pipStream = this.pipVideo.srcObject;
                if (pipStream && pipStream.getTracks) {
                    pipStream.getTracks().forEach(track => track.stop());
                }
            }
            
            // 일반 비디오 스트림 재시작
            await this.initializeLocalStream();
            
            // PIP 숨김
            this.hidePip();
            
            this.isScreenSharing = false;
            this.toggleScreenShareBtn.classList.remove('screen-sharing');
            
            // 백엔드에 화면공유 중단 알림
            await this.notifyScreenShareStop();
            
            this.showToast('화면공유를 중단했습니다.');
            
        } catch (error) {
            console.error('화면공유 중단 실패:', error);
            this.showToast('화면공유 중단 중 오류가 발생했습니다.');
        }
    }

    async notifyScreenShareStart() {
        try {
            await fetch(`/api/video-call/sessions/${this.sessionData.sessionId}/screen-share/start`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ connectionId: this.session.connection.connectionId })
            });
        } catch (error) {
            console.error('화면공유 시작 알림 실패:', error);
        }
    }

    async notifyScreenShareStop() {
        try {
            await fetch(`/api/video-call/sessions/${this.sessionData.sessionId}/screen-share/stop`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ connectionId: this.session.connection.connectionId })
            });
        } catch (error) {
            console.error('화면공유 중단 알림 실패:', error);
        }
    }

    async sendChatMessage() {
        const message = this.chatInput.value.trim();
        if (!message) return;
        
        try {
            // 실시간 채팅 전송 (OpenVidu signal)
            await this.session.signal({
                data: JSON.stringify({
                    username: this.sessionData.username,
                    message: message
                }),
                type: 'chat'
            });
            
            // 자신의 메시지 표시
            this.displayChatMessage(this.sessionData.username, message, true);
            
            // 데이터베이스에 저장
            await this.saveChatMessage(message);
            
            this.chatInput.value = '';
            
        } catch (error) {
            console.error('채팅 메시지 전송 실패:', error);
            this.showToast('메시지 전송에 실패했습니다.');
        }
    }

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

    async loadChatHistory() {
        try {
            const response = await fetch(`/api/chat/sessions/${this.sessionData.sessionId}/messages`);
            const messages = await response.json();
            
            messages.forEach(msg => {
                this.displayChatMessage(msg.username, msg.message, msg.username === this.sessionData.username);
            });
            
        } catch (error) {
            console.error('채팅 기록 로드 실패:', error);
        }
    }

    displayChatMessage(username, message, isOwn = false) {
        const messageEl = document.createElement('div');
        messageEl.className = `chat-message ${isOwn ? 'own' : ''}`;
        
        const currentTime = new Date().toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
        
        messageEl.innerHTML = `
            <div class="message-avatar">${this.getUserAvatar(username)}</div>
            <div class="message-content">
                <div class="message-header">
                    <span class="message-username">${username}</span>
                    <span class="message-time">${currentTime}</span>
                </div>
                <div class="message-text">${this.escapeHtml(message)}</div>
            </div>
        `;
        
        this.chatMessages.appendChild(messageEl);
        this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    startCallTimer() {
        this.callStartTime = new Date();
        this.callDurationTimer = setInterval(() => {
            const now = new Date();
            const duration = Math.floor((now - this.callStartTime) / 1000);
            const minutes = Math.floor(duration / 60);
            const seconds = duration % 60;
            this.callDurationEl.textContent = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
        }, 1000);
    }

    stopCallTimer() {
        if (this.callDurationTimer) {
            clearInterval(this.callDurationTimer);
            this.callDurationTimer = null;
        }
    }

    async leaveSession() {
        try {
            if (this.session) {
                this.session.disconnect();
            }
            
            this.stopCallTimer();
            
            // 세션 나가기 API 호출
            await fetch(`/api/video-call/sessions/${this.sessionData.sessionId}/leave`, {
                method: 'DELETE',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ connectionId: this.session?.connection?.connectionId })
            });
            
            // 메인 페이지로 이동
            window.location.href = '/';
            
        } catch (error) {
            console.error('세션 나가기 실패:', error);
            window.location.href = '/';
        }
    }

    showLoadingScreen(show) {
        this.loadingScreen.style.display = show ? 'flex' : 'none';
    }

    showToast(message) {
        this.toastMessage.textContent = message;
        this.toast.style.display = 'block';
        setTimeout(() => {
            this.toast.style.display = 'none';
        }, 3000);
    }

    // Picture-in-Picture 관련 메소드들
    showPip(stream, username) {
        try {
            console.log('showPip 호출됨:', { stream, username });
            
            this.pipStream = stream;
            this.pipUsername = username;
            this.pipEnabled = true;
            
            // PIP 비디오에 스트림 설정 - 다양한 방법으로 시도
            let mediaStream = null;
            
            if (stream) {
                // 방법 1: 직접 MediaStream 객체인 경우 (우선순위 높음)
                if (stream instanceof MediaStream) {
                    mediaStream = stream;
                    console.log('방법 1: 직접 MediaStream 사용');
                }
                // 방법 2: getMediaStream() 메소드 사용
                else if (typeof stream.getMediaStream === 'function') {
                    mediaStream = stream.getMediaStream();
                    console.log('방법 2: getMediaStream() 사용');
                }
                // 방법 3: video element의 srcObject에서 가져오기
                else if (stream.video && stream.video.srcObject) {
                    mediaStream = stream.video.srcObject;
                    console.log('방법 3: video element srcObject 사용');
                }
                // 방법 4: OpenVidu Stream 객체에서 직접 접근
                else if (stream.streamManager && stream.streamManager.videos && stream.streamManager.videos.length > 0) {
                    mediaStream = stream.streamManager.videos[0].video.srcObject;
                    console.log('방법 4: streamManager 사용');
                }
                
                console.log('추출된 MediaStream:', mediaStream);
            }
            
            if (mediaStream && mediaStream.getTracks && mediaStream.getTracks().length > 0) {
                this.pipVideo.srcObject = mediaStream;
                this.pipVideo.muted = true; // PIP는 항상 음소거
                this.pipLabel.textContent = username;
                this.pipContainer.classList.add('show');
                
                // 비디오 재생 시작
                this.pipVideo.onloadedmetadata = () => {
                    console.log('PIP 비디오 메타데이터 로드됨');
                    this.pipVideo.play().catch(e => console.warn('PIP 자동 재생 실패:', e));
                };
                
                console.log('PIP 표시 성공:', username, mediaStream);
                console.log('MediaStream 트랙 개수:', mediaStream.getTracks().length);
            } else {
                console.warn('PIP 스트림이 유효하지 않음. 스트림 정보:', stream);
                
                // 대안: 현재 publisher의 video element에서 직접 복사
                if (this.publisher && this.publisher.videos && this.publisher.videos.length > 0) {
                    const publisherVideo = this.publisher.videos[0].video;
                    if (publisherVideo && publisherVideo.srcObject) {
                        this.pipVideo.srcObject = publisherVideo.srcObject;
                        this.pipVideo.muted = true;
                        this.pipLabel.textContent = username;
                        this.pipContainer.classList.add('show');
                        console.log('PIP 대안 방법으로 표시됨:', username);
                        return;
                    }
                }
                
                console.error('모든 PIP 표시 방법 실패');
                // 그래도 실패하면 PIP 숨김
                this.hidePip();
            }
        } catch (error) {
            console.error('PIP 표시 오류:', error);
            this.hidePip();
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
        
        this.pipEnabled = false;
        this.pipStream = null;
        this.pipUsername = null;
        this.pipVideo.srcObject = null;
        this.pipContainer.classList.remove('show');
        console.log('PIP 숨김');
    }

    togglePip() {
        if (this.pipEnabled) {
            this.hidePip();
        } else if (this.isScreenSharing && this.publisher && this.publisher.stream) {
            // 화면공유 중일 때만 PIP 토글 가능
            this.showPip(this.publisher.stream, this.sessionData.username);
        }
    }

    swapPipAndMain() {
        if (!this.pipEnabled || !this.pipVideo.srcObject) {
            console.warn('PIP가 비활성화되어 있거나 스트림이 없습니다');
            return;
        }

        try {
            console.log('PIP와 메인 화면 교체 시작');
            
            if (this.isScreenSharing) {
                // 화면공유 중일 때: PIP의 캠을 메인 화면에, 화면공유를 PIP에 표시
                const pipCamStream = this.pipVideo.srcObject;
                const mainScreenShareStream = this.mainVideo.srcObject;
                const screenShareLabel = this.mainVideoLabel.textContent;
                
                console.log('교체 대상:', { pipCamStream, mainScreenShareStream });
                
                // 메인 화면을 PIP의 캠으로 변경
                this.mainVideo.srcObject = pipCamStream;
                this.mainVideoOverlay.style.display = 'none';
                this.mainVideoLabel.textContent = this.pipUsername;
                this.currentMainVideo = { getMediaStream: () => pipCamStream };
                
                // PIP를 화면공유로 변경
                if (mainScreenShareStream) {
                    this.pipVideo.srcObject = mainScreenShareStream;
                    this.pipLabel.textContent = screenShareLabel;
                    this.pipStream = mainScreenShareStream;
                    this.pipUsername = screenShareLabel;
                }
                
                console.log('교체 완료');
                this.showToast('캠과 화면공유가 교체되었습니다.');
            } else {
                // 일반 상황에서의 교체 (현재는 PIP가 화면공유 중에만 표시되므로 여기는 실행되지 않음)
                console.warn('화면공유가 활성화되지 않음');
                this.showToast('화면공유 중에만 교체할 수 있습니다.');
            }
            
        } catch (error) {
            console.error('화면 교체 오류:', error);
            this.showToast('화면 교체 중 오류가 발생했습니다.');
        }
    }

    // 드래그 앤 드롭 기능
    startDragging(e) {
        if (e.target.closest('.pip-control-btn')) return; // 컨트롤 버튼 클릭 시 드래그 방지
        
        this.isDragging = true;
        this.pipContainer.classList.add('dragging');
        
        const rect = this.pipContainer.getBoundingClientRect();
        this.dragOffset = {
            x: e.clientX - rect.left,
            y: e.clientY - rect.top
        };
        
        e.preventDefault();
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
        
        // 스냅 처리 (모서리 근처에 있으면 자동으로 스냅)
        this.snapToEdge();
    }

    snapToEdge() {
        const containerRect = this.pipContainer.parentElement.getBoundingClientRect();
        const pipRect = this.pipContainer.getBoundingClientRect();
        const snapThreshold = 50; // 스냅 감지 거리
        
        const relativeRect = {
            left: pipRect.left - containerRect.left,
            top: pipRect.top - containerRect.top,
            right: containerRect.width - (pipRect.right - containerRect.left),
            bottom: containerRect.height - (pipRect.bottom - containerRect.top)
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
            this.pipContainer.style.bottom = '20px';
        }
    }
}

// 페이지 로드 시 화상통화 매니저 초기화
document.addEventListener('DOMContentLoaded', () => {
    // OpenVidu 서버 URL 전역 설정 확인 및 재설정
    console.log('Current OPENVIDU_SERVER_URL:', window.OPENVIDU_SERVER_URL);
    
    // WebSocket URL을 HTTP URL에서 WS URL로 변환
    if (window.OPENVIDU_SERVER_URL && window.OPENVIDU_SERVER_URL.includes('http://localhost:25565')) {
        window.OPENVIDU_WEBSOCKET_URL = 'ws://localhost:25565/openvidu';
        console.log('Set OPENVIDU_WEBSOCKET_URL:', window.OPENVIDU_WEBSOCKET_URL);
    }
    
    new NewVideoCallManager();
});