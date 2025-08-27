// OpenVidu 화상통화 ver3 JavaScript  
// CremaChat UI - v2 로직을 완전히 이식하여 듀얼 화면 인터페이스로 구현
// 🔄 버전: 2025-08-27-v3-dual-screen

// 버전 정보 출력
console.log('======================================');
console.log('🚀 CremaChat VideoCall Script Version: v3-dual-screen-2025-08-27');
console.log('✅ 듀얼 화면 레이아웃 지원');
console.log('✅ 모든 v2 기능 완벽 이식');
console.log('======================================');

class NewVideoCallV3Manager {
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
        
        // 듀얼 화면 관리
        this.leftVideoStream = null;
        this.rightVideoStream = null;
        this.leftUsername = null;
        this.rightUsername = null;
        
        // UI 요소들
        this.leftVideo = null;
        this.rightVideo = null;
        this.leftVideoOverlay = null;
        this.rightVideoOverlay = null;
        
        // 사이드바 탭 관리
        this.currentTab = 'chat'; // 'chat' 또는 'files'
        this.chatMessageCount = 0;
        
        // 타이머 관련
        this.callStartTime = null;
        this.callTimer = null;
        
        // 참가자 관리
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
            
            console.log('✅ CremaChat v3 앱 초기화 성공 - 모든 시스템 정상 작동');
            
        } catch (error) {
            console.error('❌ 앱 초기화 실패:', error);
            this.showToast('연결에 실패했습니다. 페이지를 새로고침해주세요.');
        }
    }
    
    initializeElements() {
        // 듀얼 비디오 요소들
        this.leftVideo = document.getElementById('leftVideo');
        this.rightVideo = document.getElementById('rightVideo');
        this.leftVideoOverlay = document.getElementById('leftVideoOverlay');
        this.rightVideoOverlay = document.getElementById('rightVideoOverlay');
        this.leftUserTag = document.getElementById('leftUserTag');
        this.rightUserTag = document.getElementById('rightUserTag');
        
        // 컨트롤 버튼들
        this.toggleAudioBtn = document.getElementById('toggleAudioBtn');
        this.toggleVideoBtn = document.getElementById('toggleVideoBtn');
        this.toggleScreenShareBtn = document.getElementById('toggleScreenShareBtn');
        this.leaveSessionBtn = document.getElementById('leaveSessionBtn');
        this.settingsBtn = document.getElementById('settingsBtn');
        
        // 헤더 탭 버튼들
        this.chatTabBtn = document.getElementById('chatTabBtn');
        this.filesTabBtn = document.getElementById('filesTabBtn');
        this.chatBadge = document.getElementById('chatBadge');
        
        // 사이드바 섹션들
        this.chatSection = document.getElementById('chatSection');
        this.filesSection = document.getElementById('filesSection');
        
        // 채팅 관련 요소들
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
        
        // 숨겨진 데이터 컨테이너
        this.participantsList = document.getElementById('participantsList');
        this.participantCount = document.getElementById('participantCount');
    }
    
    attachEventListeners() {
        // 컨트롤 버튼 이벤트
        this.toggleAudioBtn.addEventListener('click', () => this.toggleAudio());
        this.toggleVideoBtn.addEventListener('click', () => this.toggleVideo());
        this.toggleScreenShareBtn.addEventListener('click', () => this.toggleScreenShare());
        this.leaveSessionBtn.addEventListener('click', () => this.leaveSession());
        this.settingsBtn.addEventListener('click', () => this.showSettings());
        
        // 헤더 탭 이벤트
        this.chatTabBtn.addEventListener('click', () => this.switchTab('chat'));
        this.filesTabBtn.addEventListener('click', () => this.switchTab('files'));
        
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
        
        // 키보드 단축키
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                // ESC 키로 설정 메뉴 등 닫기
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
        // OpenVidu 서버 URL 명시적 설정
        const openViduServerUrl = 'https://crema.bitcointothemars.com/openvidu';
        window.OPENVIDU_SERVER_URL = openViduServerUrl;
        
        console.log('🔧 OpenVidu Server URL configured:', openViduServerUrl);
        console.log('🔧 Protocol:', window.location.protocol);
        console.log('🔧 Host:', window.location.host);
        
        // WebRTC 지원 확인
        if (!window.RTCPeerConnection) {
            throw new Error('이 브라우저는 WebRTC를 지원하지 않습니다.');
        }
    }
    
    async connectToSession() {
        this.showLoading(true);
        
        try {
            // OpenVidu 객체 생성 전 환경 확인
            if (typeof OpenVidu === 'undefined' && typeof window.OpenVidu === 'undefined') {
                throw new Error('OpenVidu 라이브러리가 로드되지 않았습니다.');
            }
            
            // OpenVidu 객체 생성
            this.OV = new OpenVidu();
            this.session = this.OV.initSession();
            
            console.log('🔧 OpenVidu Session initialized');
            
            // 세션 이벤트 리스너 설정
            this.setupSessionEventListeners();
            
            // 세션 연결
            console.log('🔧 Connecting to session with token:', this.sessionData.token);
            
            await this.session.connect(this.sessionData.token, this.sessionData.username);
            console.log('✅ 세션 연결 성공');
            
            // Publisher 생성 및 발행
            await this.initializePublisher();
            
            this.showLoading(false);
            this.showToast('화상통화에 연결되었습니다');
            
        } catch (error) {
            console.error('❌ 세션 연결 실패:', error);
            this.showLoading(false);
            this.showToast('연결에 실패했습니다: ' + error.message);
            throw error;
        }
    }
    
    setupSessionEventListeners() {
        // 스트림 생성 이벤트
        this.session.on('streamCreated', (event) => {
            console.log('🎥 새 스트림 생성됨:', event);
            console.log('📋 스트림 정보:', {
                streamId: event.stream.streamId,
                connectionId: event.stream.connection.connectionId,
                hasVideo: event.stream.hasVideo,
                hasAudio: event.stream.hasAudio
            });
            
            // 사용자명 추출 및 표시
            const username = event.stream.connection.data.split('%')[0];
            console.log('👤 구독자 사용자명:', username);
            
            // Subscriber 생성 시 왼쪽 비디오 요소를 직접 지정
            const subscriber = this.session.subscribe(event.stream, this.leftVideo);
            this.subscribers.push(subscriber);
            console.log('✅ Subscriber 생성됨, 왼쪽 비디오에 연결');
            
            // 추가적으로 addVideoElement도 시도
            try {
                subscriber.addVideoElement(this.leftVideo);
                console.log('✅ addVideoElement로 비디오 요소 추가됨');
            } catch (error) {
                console.log('⚠️ addVideoElement 실패 (정상적일 수 있음):', error.message);
            }
            
            // 타임아웃으로 강제 연결 시도 (2초 후)
            setTimeout(() => {
                console.log('⏰ 타임아웃 - 강제 스트림 연결 시도');
                try {
                    if (this.leftVideo.srcObject === null && subscriber.stream) {
                        this.leftVideo.srcObject = subscriber.stream.getMediaStream();
                        this.leftVideo.muted = false;  // 음소거 해제
                        this.leftVideo.play();  // 비디오 재생 시작
                        this.leftVideoStream = subscriber.stream;
                        this.leftUsername = username;
                        this.leftUserTag.textContent = username;
                        this.leftVideoOverlay.classList.add('hidden');
                        console.log('✅ 타임아웃 강제 연결 성공');
                    } else if (this.leftVideo.srcObject !== null) {
                        console.log('✅ 이미 스트림이 연결되어 있음');
                        // 이미 연결되어 있어도 재생 시도
                        try {
                            this.leftVideo.play();
                        } catch (playError) {
                            console.log('⚠️ 비디오 재생 시도 실패:', playError.message);
                        }
                    }
                } catch (error) {
                    console.error('❌ 타임아웃 강제 연결 실패:', error);
                }
            }, 2000);
            
            subscriber.on('videoElementCreated', (videoEvent) => {
                console.log('✅ 구독자 비디오 요소 생성됨');
                
                // 왼쪽 슬롯 설정
                this.leftVideoStream = subscriber.stream;
                this.leftUsername = username;
                this.leftUserTag.textContent = username;
                this.leftVideoOverlay.classList.add('hidden');
                
                // 즉시 스트림 연결 시도
                console.log('🔧 즉시 스트림 연결 시도');
                try {
                    if (subscriber.stream && subscriber.stream.getMediaStream()) {
                        this.leftVideo.srcObject = subscriber.stream.getMediaStream();
                        this.leftVideo.muted = false;  // 음소거 해제 (상대방 오디오를 들을 수 있도록)
                        this.leftVideo.play();  // 비디오 재생 시작
                        console.log('✅ 즉시 스트림 연결 성공');
                    } else {
                        console.log('⚠️ 스트림이 아직 준비되지 않음');
                    }
                } catch (error) {
                    console.error('❌ 즉시 스트림 연결 실패:', error);
                }
            });
            
            subscriber.on('streamReady', () => {
                console.log('✅ 구독자 스트림 준비됨');
                console.log('🔄 스트림 준비 완료 - 사용자:', username);
                
                // 스트림이 준비되면 다시 한번 확실하게 설정
                this.leftVideoStream = subscriber.stream;
                this.leftUsername = username;
                this.leftUserTag.textContent = username;
                this.leftVideoOverlay.classList.add('hidden');
                
                // 추가: 수동으로 비디오 요소에 스트림 연결 (혹시 자동 연결이 안된 경우 대비)
                if (this.leftVideo.srcObject === null) {
                    console.log('🔧 수동으로 비디오 스트림 연결 시도');
                    try {
                        this.leftVideo.srcObject = subscriber.stream.getMediaStream();
                        console.log('✅ 수동 비디오 스트림 연결 성공');
                    } catch (error) {
                        console.error('❌ 수동 비디오 스트림 연결 실패:', error);
                    }
                }
            });
        });
        
        // 스트림 삭제 이벤트
        this.session.on('streamDestroyed', (event) => {
            console.log('🎥 스트림 삭제됨:', event);
            this.removeVideoFromSlot(event.stream);
            this.subscribers = this.subscribers.filter(sub => sub.stream !== event.stream);
        });
        
        // 참가자 연결 이벤트
        this.session.on('connectionCreated', (event) => {
            console.log('👤 새 참가자 연결됨:', event);
            const username = event.connection.data.split('%')[0];
            this.participants.set(event.connection.connectionId, {
                username: username,
                connectionId: event.connection.connectionId,
                isMe: event.connection === this.session.connection
            });
            this.updateParticipantCount();
        });
        
        // 참가자 연결 해제 이벤트
        this.session.on('connectionDestroyed', (event) => {
            console.log('👤 참가자 연결 해제됨:', event);
            this.participants.delete(event.connection.connectionId);
            this.updateParticipantCount();
        });
        
        // 채팅 메시지 수신 이벤트
        this.session.on('signal:chat', (event) => {
            this.handleChatMessage(event);
        });
        
        // 파일 공유 수신 이벤트
        this.session.on('signal:file', (event) => {
            this.handleFileMessage(event);
        });
        
        // 세션 재연결 이벤트
        this.session.on('reconnecting', () => {
            console.log('🔄 세션 재연결 중...');
            this.showToast('연결이 끊어져 재연결 중입니다...');
        });
        
        this.session.on('reconnected', () => {
            console.log('✅ 세션 재연결 성공');
            this.showToast('연결이 복구되었습니다');
        });
    }
    
    async initializePublisher() {
        try {
            console.log('🎥 Publisher 초기화 시작');
            
            // Publisher 생성
            this.publisher = await this.OV.initPublisherAsync(undefined, {
                audioSource: undefined,
                videoSource: undefined,
                publishAudio: true,
                publishVideo: true,
                resolution: '640x480',
                frameRate: 30,
                insertMode: 'APPEND',
                mirror: true
            });
            
            console.log('✅ Publisher 생성 완료');
            
            // Publisher를 세션에 발행
            await this.session.publish(this.publisher);
            console.log('✅ Publisher 발행 완료');
            
            // 내 비디오를 오른쪽 슬롯에 배치
            this.assignVideoToSlot(this.publisher.stream, this.sessionData.username, true);
            
            // Publisher 이벤트 리스너
            this.publisher.on('streamReady', () => {
                console.log('✅ Publisher 스트림 준비됨');
            });
            
        } catch (error) {
            console.error('❌ Publisher 초기화 실패:', error);
            throw error;
        }
    }
    
    // 듀얼 화면에 비디오 스트림 할당
    assignVideoToSlot(stream, username, isLocal = false) {
        console.log('🎯 비디오 슬롯 할당:', username, isLocal ? '(로컬)' : '(원격)');
        
        if (isLocal) {
            // 로컬 스트림을 오른쪽에 배치
            this.rightVideo.srcObject = stream.getMediaStream();
            this.rightVideo.muted = true;  // 자신의 오디오는 음소거 (에코 방지)
            this.rightVideo.play();  // 비디오 재생 시작
            this.rightVideoStream = stream;
            this.rightUsername = username;
            this.rightUserTag.textContent = `${username} (나)`;
            this.rightVideoOverlay.classList.add('hidden');
        } else {
            // 원격 스트림을 왼쪽에 배치
            this.leftVideo.srcObject = stream.getMediaStream();
            this.leftVideoStream = stream;
            this.leftUsername = username;
            this.leftUserTag.textContent = username;
            this.leftVideoOverlay.classList.add('hidden');
        }
    }
    
    // 비디오 슬롯에서 스트림 제거
    removeVideoFromSlot(stream) {
        if (this.leftVideoStream === stream) {
            this.leftVideo.srcObject = null;
            this.leftVideoStream = null;
            this.leftUsername = null;
            this.leftUserTag.textContent = '';
            this.leftVideoOverlay.classList.remove('hidden');
        } else if (this.rightVideoStream === stream) {
            this.rightVideo.srcObject = null;
            this.rightVideoStream = null;
            this.rightUsername = null;
            this.rightUserTag.textContent = '';
            this.rightVideoOverlay.classList.remove('hidden');
        }
    }
    
    // 오디오 토글
    toggleAudio() {
        if (this.publisher) {
            const enabled = this.publisher.publishAudio;
            this.publisher.publishAudio(!enabled);
            
            if (enabled) {
                this.toggleAudioBtn.classList.add('disabled');
                this.toggleAudioBtn.querySelector('i').className = 'fas fa-microphone-slash';
            } else {
                this.toggleAudioBtn.classList.remove('disabled');
                this.toggleAudioBtn.querySelector('i').className = 'fas fa-microphone';
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
                this.toggleVideoBtn.querySelector('i').className = 'fas fa-video-slash';
                this.rightVideoOverlay.classList.remove('hidden');
            } else {
                this.toggleVideoBtn.classList.remove('disabled');
                this.toggleVideoBtn.querySelector('i').className = 'fas fa-video';
                this.rightVideoOverlay.classList.add('hidden');
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
                publishAudio: screenStream.getAudioTracks().length > 0,
                mirror: false
            });
            
            // 기존 Publisher를 화면공유 Publisher로 교체
            if (this.publisher) {
                await this.session.unpublish(this.publisher);
            }
            
            await this.session.publish(this.screenSharePublisher);
            
            // 화면공유를 오른쪽 슬롯에 표시
            this.assignVideoToSlot(this.screenSharePublisher.stream, `${this.sessionData.username} (화면공유)`, true);
            
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
            
            // 기존 Publisher 다시 발행
            if (this.publisher) {
                await this.session.publish(this.publisher);
                this.assignVideoToSlot(this.publisher.stream, this.sessionData.username, true);
            }
            
            // 상태 업데이트
            this.isScreenSharing = false;
            this.toggleScreenShareBtn.classList.remove('active');
            
            this.showToast('화면공유가 중단되었습니다');
            console.log('화면공유 중단 완료');
            
        } catch (error) {
            console.error('화면공유 중단 실패:', error);
            this.showToast('화면공유 중단에 실패했습니다');
        }
    }
    
    // 탭 전환 (채팅 <-> 공유자료)
    switchTab(tabName) {
        console.log('🔄 탭 전환:', tabName);
        
        if (tabName === 'chat') {
            this.currentTab = 'chat';
            this.chatTabBtn.classList.add('active');
            this.filesTabBtn.classList.remove('active');
            this.chatSection.style.display = 'flex';
            this.filesSection.style.display = 'none';
        } else if (tabName === 'files') {
            this.currentTab = 'files';
            this.chatTabBtn.classList.remove('active');
            this.filesTabBtn.classList.add('active');
            this.chatSection.style.display = 'none';
            this.filesSection.style.display = 'flex';
        }
    }
    
    // 채팅 메시지 전송
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
    
    // 채팅 메시지 수신 처리
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
            
            // 채팅 메시지 개수 업데이트
            if (!isMyMessage) {
                this.chatMessageCount++;
                this.updateChatBadge();
            }
            
            // 채팅 탭이 아닌 경우 알림
            if (this.currentTab !== 'chat' && !isMyMessage) {
                this.showToast(`${data.username}님이 메시지를 보냈습니다`);
            }
            
        } catch (error) {
            console.error('채팅 메시지 처리 실패:', error);
        }
    }
    
    // 파일 업로드 처리
    async handleFileUpload(file) {
        console.log('🔧 파일 업로드 시작:', file.name, file.size, file.type);
        
        // 파일 크기 제한 (2MB)
        const maxFileSize = 2 * 1024 * 1024;
        if (file.size > maxFileSize) {
            this.showToast(`파일 크기는 ${this.formatFileSize(maxFileSize)}를 초과할 수 없습니다`);
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
            return;
        }
        
        try {
            this.showToast(`파일 "${file.name}" 업로드 중...`);
            
            // Base64 인코딩
            const base64Data = await this.fileToBase64(file);
            
            // 파일 정보를 시그널로 전송
            await this.session.signal({
                type: 'file',
                data: JSON.stringify({
                    filename: file.name,
                    filesize: file.size,
                    filetype: file.type,
                    filedata: base64Data,
                    username: this.sessionData.username,
                    timestamp: Date.now()
                })
            });
            
            this.showToast('파일이 공유되었습니다');
            console.log('✅ 파일 업로드 완료');
            
        } catch (error) {
            console.error('❌ 파일 업로드 실패:', error);
            this.showToast('파일 업로드에 실패했습니다');
        }
    }
    
    // 파일 메시지 처리
    handleFileMessage(event) {
        try {
            const data = JSON.parse(event.data);
            console.log('📁 파일 메시지 수신:', data.filename);
            
            // 파일을 공유자료 섹션에 추가
            const fileEl = document.createElement('div');
            fileEl.className = 'shared-file-item';
            fileEl.innerHTML = `
                <div class="file-info">
                    <span class="file-name">${data.filename}</span>
                    <span class="file-size">${this.formatFileSize(data.filesize)}</span>
                </div>
                <button onclick="downloadFile('${data.filedata}', '${data.filename}')" class="download-btn">
                    <i class="fas fa-download"></i>
                </button>
            `;
            
            // 파일을 공유자료 영역에 추가
            const sharedFiles = document.getElementById('sharedFiles');
            if (sharedFiles) {
                sharedFiles.appendChild(fileEl);
            }
            
            // 채팅에도 파일 공유 메시지 추가
            const chatMessage = document.createElement('div');
            chatMessage.className = 'chat-message file-message';
            chatMessage.innerHTML = `
                <div class="chat-message-header">
                    <span class="chat-username">${data.username}</span>
                    <span class="chat-timestamp">${new Date(data.timestamp).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}</span>
                </div>
                <div class="chat-message-content">
                    📎 파일을 공유했습니다: ${data.filename} (${this.formatFileSize(data.filesize)})
                </div>
            `;
            
            this.chatMessages.appendChild(chatMessage);
            this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
            
        } catch (error) {
            console.error('파일 메시지 처리 실패:', error);
        }
    }
    
    // 세션 종료
    async leaveSession() {
        console.log('🚪 세션 종료 시도');
        
        try {
            if (this.session) {
                await this.session.disconnect();
            }
            
            this.cleanup();
            
            // 메인 페이지로 리디렉션
            window.location.href = '/';
            
        } catch (error) {
            console.error('세션 종료 실패:', error);
            // 에러가 발생해도 페이지 리디렉션 수행
            window.location.href = '/';
        }
    }
    
    // 설정 메뉴 표시
    showSettings() {
        this.showToast('설정 기능은 준비 중입니다');
    }
    
    // 통화 시간 타이머 시작
    startCallTimer() {
        this.callStartTime = Date.now();
        this.callTimer = setInterval(() => {
            const elapsed = Date.now() - this.callStartTime;
            const minutes = Math.floor(elapsed / 60000);
            const seconds = Math.floor((elapsed % 60000) / 1000);
            this.callDuration.textContent = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
        }, 1000);
    }
    
    // 참가자 수 업데이트
    updateParticipantCount() {
        const count = this.participants.size;
        this.participantCount.textContent = count.toString();
        console.log('👥 참가자 수 업데이트:', count);
    }
    
    // 채팅 배지 업데이트
    updateChatBadge() {
        this.chatBadge.textContent = this.chatMessageCount.toString();
        if (this.chatMessageCount > 0 && this.currentTab !== 'chat') {
            this.chatBadge.style.display = 'inline';
        } else {
            this.chatBadge.style.display = 'none';
            if (this.currentTab === 'chat') {
                this.chatMessageCount = 0;
                this.chatBadge.textContent = '0';
            }
        }
    }
    
    // 유틸리티 메서드들
    
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
    
    fileToBase64(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.readAsDataURL(file);
            reader.onload = () => resolve(reader.result.split(',')[1]);
            reader.onerror = error => reject(error);
        });
    }
    
    showLoading(show) {
        if (show) {
            this.loadingScreen.style.display = 'flex';
        } else {
            this.loadingScreen.style.display = 'none';
        }
    }
    
    showToast(message, duration = 3000) {
        this.toastMessage.textContent = message;
        this.toast.style.display = 'block';
        
        setTimeout(() => {
            this.toast.style.display = 'none';
        }, duration);
        
        console.log('📢 토스트:', message);
    }
    
    cleanup() {
        console.log('🧹 리소스 정리 시작');
        
        // 타이머 정리
        if (this.callTimer) {
            clearInterval(this.callTimer);
            this.callTimer = null;
        }
        
        // 세션 정리
        if (this.session) {
            this.session = null;
        }
        
        // OpenVidu 객체 정리
        if (this.OV) {
            this.OV = null;
        }
        
        console.log('✅ 리소스 정리 완료');
    }
}

// 전역 함수들

// 파일 다운로드 함수
function downloadFile(base64Data, filename) {
    try {
        const byteCharacters = atob(base64Data);
        const byteNumbers = new Array(byteCharacters.length);
        for (let i = 0; i < byteCharacters.length; i++) {
            byteNumbers[i] = byteCharacters.charCodeAt(i);
        }
        const byteArray = new Uint8Array(byteNumbers);
        const blob = new Blob([byteArray]);
        
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        
        console.log('✅ 파일 다운로드:', filename);
    } catch (error) {
        console.error('❌ 파일 다운로드 실패:', error);
        alert('파일 다운로드에 실패했습니다.');
    }
}

// 앱 시작
document.addEventListener('DOMContentLoaded', function() {
    console.log('📱 CremaChat v3 초기화 시작...');
    
    // 전역 앱 인스턴스 생성
    window.videoCallApp = new NewVideoCallV3Manager();
    
    console.log('✅ CremaChat v3 앱 인스턴스 생성 완료');
});