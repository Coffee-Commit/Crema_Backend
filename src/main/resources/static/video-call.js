// 화상통화 페이지 JavaScript

class VideoCallManager {
    constructor() {
        this.session = null;
        this.publisher = null;
        this.subscribers = [];
        this.sessionData = null;
        this.localVideoEnabled = true;
        this.localAudioEnabled = true;
        
        this.initializeElements();
        this.loadSessionData();
        this.attachEventListeners();
        
        // 토큰 디버깅
        console.log('Original token:', this.sessionData.token);
        
        this.initializeSession();
    }

    initializeElements() {
        this.videoGrid = document.getElementById('video-grid');
        this.localVideo = document.getElementById('localVideo');
        this.sessionIdDisplay = document.getElementById('sessionIdDisplay');
        this.usernameDisplay = document.getElementById('usernameDisplay');
        this.toggleVideoBtn = document.getElementById('toggleVideo');
        this.toggleAudioBtn = document.getElementById('toggleAudio');
        this.leaveSessionBtn = document.getElementById('leaveSession');
        this.chatMessages = document.getElementById('chatMessages');
        this.messageInput = document.getElementById('messageInput');
        this.sendMessageBtn = document.getElementById('sendMessage');
        this.loading = document.getElementById('loading');
    }

    loadSessionData() {
        const sessionDataStr = localStorage.getItem('videoCallSession');
        if (!sessionDataStr) {
            alert('세션 정보가 없습니다. 메인 페이지로 이동합니다.');
            window.location.href = '/';
            return;
        }
        
        this.sessionData = JSON.parse(sessionDataStr);
        this.updateDisplays();
    }

    updateDisplays() {
        this.sessionIdDisplay.textContent = `세션: ${this.sessionData.sessionId}`;
        this.usernameDisplay.textContent = `사용자: ${this.sessionData.username}`;
    }

    attachEventListeners() {
        this.toggleVideoBtn.addEventListener('click', () => this.toggleVideo());
        this.toggleAudioBtn.addEventListener('click', () => this.toggleAudio());
        this.leaveSessionBtn.addEventListener('click', () => this.leaveSession());
        this.sendMessageBtn.addEventListener('click', () => this.sendChatMessage());
        
        this.messageInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.sendChatMessage();
        });

        // 페이지 언로드 시 세션 정리
        window.addEventListener('beforeunload', () => {
            this.leaveSession();
        });
    }

    async initializeSession() {
        try {
            this.showLoading(true);
            
            // 토큰 유효성 검사
            if (!this.sessionData.token) {
                throw new Error('토큰이 없습니다.');
            }

            // OpenVidu 객체 생성 - 올바른 서버 URL로 설정
            this.OV = new OpenVidu();
            
            // OpenVidu 서버 설정 - WebSocket URL 직접 오버라이드
            const originalConnect = this.OV.initSession().connect;
            
            // WebSocket URL을 강제로 변경
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
            
            this.session = this.OV.initSession();

            // 세션 이벤트 리스너 설정
            this.setupSessionEvents();

            // 세션 연결 - 서버 URL을 명시적으로 지정
            const token = this.sessionData.token;
            const connectionData = JSON.stringify({
                username: this.sessionData.username
            });

            // 원본 토큰 사용 (WebSocket URL은 위에서 리디렉션됨)
            console.log('Using original token:', token);
            
            await this.session.connect(token, connectionData);

            // 퍼블리셔 생성 및 발행
            await this.createPublisher();
            
            this.showLoading(false);
            this.addChatMessage('시스템', '화상통화에 연결되었습니다.');
            
        } catch (error) {
            console.error('세션 초기화 오류:', error);
            console.error('Error details:', {
                name: error.name,
                message: error.message,
                stack: error.stack
            });
            alert('화상통화 연결에 실패했습니다: ' + (error.message || '알 수 없는 오류'));
            this.showLoading(false);
        }
    }

    setupSessionEvents() {
        // 새로운 스트림이 생성되었을 때
        this.session.on('streamCreated', (event) => {
            const subscriber = this.session.subscribe(event.stream, undefined);
            this.subscribers.push(subscriber);
            this.addVideoElement(subscriber, this.getStreamUsername(event.stream));
            
            const username = this.getStreamUsername(event.stream);
            this.addChatMessage('시스템', `${username}님이 입장했습니다.`);
        });

        // 스트림이 제거되었을 때
        this.session.on('streamDestroyed', (event) => {
            const username = this.getStreamUsername(event.stream);
            this.removeVideoElement(event.stream.streamId);
            this.addChatMessage('시스템', `${username}님이 나갔습니다.`);
        });

        // 시그널 메시지 수신 (채팅)
        this.session.on('signal:chat', (event) => {
            const data = JSON.parse(event.data);
            this.addChatMessage(data.username, data.message);
        });

        // 연결 오류
        this.session.on('exception', (exception) => {
            console.error('세션 예외:', exception);
        });
    }

    async createPublisher() {
        this.publisher = await this.OV.initPublisher(undefined, {
            audioSource: undefined,
            videoSource: undefined,
            publishAudio: this.localAudioEnabled,
            publishVideo: this.localVideoEnabled,
            resolution: '640x480',
            frameRate: 30,
            insertMode: 'APPEND',
            mirror: false
        });

        await this.session.publish(this.publisher);
        
        // 로컬 비디오를 지정된 요소에 추가
        this.publisher.addVideoElement(this.localVideo);
    }

    addVideoElement(subscriber, username) {
        const videoContainer = document.createElement('div');
        videoContainer.className = 'remote-video-container';
        videoContainer.id = `video-${subscriber.stream.streamId}`;
        
        const video = document.createElement('video');
        video.autoplay = true;
        video.playsinline = true;
        
        const label = document.createElement('div');
        label.className = 'video-label';
        label.textContent = username;
        
        videoContainer.appendChild(video);
        videoContainer.appendChild(label);
        this.videoGrid.appendChild(videoContainer);
        
        subscriber.addVideoElement(video);
    }

    removeVideoElement(streamId) {
        const videoElement = document.getElementById(`video-${streamId}`);
        if (videoElement) {
            videoElement.remove();
        }
    }

    getStreamUsername(stream) {
        try {
            const connectionData = JSON.parse(stream.connection.data);
            return connectionData.username || '사용자';
        } catch (e) {
            return '사용자';
        }
    }

    toggleVideo() {
        this.localVideoEnabled = !this.localVideoEnabled;
        this.publisher.publishVideo(this.localVideoEnabled);
        this.toggleVideoBtn.textContent = this.localVideoEnabled ? '📹' : '📹❌';
        this.toggleVideoBtn.classList.toggle('disabled', !this.localVideoEnabled);
    }

    toggleAudio() {
        this.localAudioEnabled = !this.localAudioEnabled;
        this.publisher.publishAudio(this.localAudioEnabled);
        this.toggleAudioBtn.textContent = this.localAudioEnabled ? '🎤' : '🎤❌';
        this.toggleAudioBtn.classList.toggle('disabled', !this.localAudioEnabled);
    }

    sendChatMessage() {
        const message = this.messageInput.value.trim();
        if (!message) return;

        const chatData = {
            username: this.sessionData.username,
            message: message,
            timestamp: new Date().toISOString()
        };

        this.session.signal({
            data: JSON.stringify(chatData),
            type: 'chat'
        }).then(() => {
            this.messageInput.value = '';
            this.addChatMessage(this.sessionData.username, message, true);
        }).catch(error => {
            console.error('메시지 전송 오류:', error);
        });
    }

    addChatMessage(username, message, isOwn = false) {
        const messageElement = document.createElement('div');
        messageElement.className = `chat-message ${isOwn ? 'own' : ''}`;
        
        const time = new Date().toLocaleTimeString('ko-KR', { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
        
        messageElement.innerHTML = `
            <div class="message-header">
                <span class="username">${username}</span>
                <span class="time">${time}</span>
            </div>
            <div class="message-content">${this.escapeHtml(message)}</div>
        `;
        
        this.chatMessages.appendChild(messageElement);
        this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    async leaveSession() {
        if (this.session) {
            try {
                await this.session.disconnect();
            } catch (error) {
                console.error('세션 종료 오류:', error);
            }
        }
        
        // 로컬 스토리지 정리
        localStorage.removeItem('videoCallSession');
        
        // 메인 페이지로 이동
        window.location.href = '/';
    }

    showLoading(show) {
        this.loading.style.display = show ? 'flex' : 'none';
    }
}

// 페이지 로드 시 화상통화 매니저 초기화
document.addEventListener('DOMContentLoaded', () => {
    new VideoCallManager();
});