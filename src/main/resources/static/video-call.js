// 화상통화 페이지 JavaScript

class VideoCallManager {
    constructor() {
        this.session = null;
        this.publisher = null;
        this.subscribers = [];
        this.sessionData = null;
        this.localVideoEnabled = true;
        this.localAudioEnabled = true;
        
        // 녹화 관련 상태
        this.isRecording = false;
        this.currentRecording = null;
        this.recordingDuration = 0;
        this.recordingTimer = null;
        this.recordings = [];
        
        // 브라우저 기반 녹화
        this.mediaRecorder = null;
        this.recordedChunks = [];
        this.audioStream = null;
        
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
        
        // 녹화 관련 요소들
        this.toggleRecordingBtn = document.getElementById('toggleRecording');
        this.recordingIndicator = document.getElementById('recordingIndicator');
        this.recordingStatusText = document.getElementById('recordingStatusText');
        this.recordingDurationElement = document.getElementById('recordingDuration');
        this.recordingList = document.getElementById('recordingList');
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
        this.toggleRecordingBtn.addEventListener('click', () => this.toggleRecording());
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
                    // ALB 경로 기반 WebSocket 연결로 변환
                    if (url.includes('localhost:4443') || url.includes('localhost')) {
                        url = url.replace(/https?:\/\/[^\/]+/, 'wss://crema.bitcointothemars.com');
                        url = url.replace(/ws:\/\/[^\/]+/, 'wss://crema.bitcointothemars.com');
                        console.log('WebSocket URL redirected to ALB:', url);
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
            
            // 기존 녹화 파일 목록 불러오기
            this.loadRecordingList();
            
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

    // ========== 새로운 음성 녹화 시스템 (OpenVidu MediaStream 직접 사용) ==========
    
    async toggleRecording() {
        try {
            if (this.isRecording) {
                await this.stopRecording();
            } else {
                await this.startRecording();
            }
        } catch (error) {
            console.error('녹화 토글 오류:', error);
            alert('녹화 기능 오류: ' + (error.message || '알 수 없는 오류'));
        }
    }

    async startRecording() {
        try {
            console.log('🎵 OpenVidu 음성 녹화 시작');
            
            // 세션 및 퍼블리셔 연결 상태 확인
            if (!this.session || !this.publisher) {
                throw new Error('화상통화에 연결되어 있지 않습니다. 먼저 화상통화를 시작해주세요.');
            }
            
            // OpenVidu publisher에서 실제 MediaStream 가져오기
            const publisherStream = this.publisher.stream.getMediaStream();
            
            if (!publisherStream) {
                throw new Error('화상통화 스트림을 가져올 수 없습니다.');
            }
            
            const audioTracks = publisherStream.getAudioTracks();
            console.log('🎤 감지된 오디오 트랙:', audioTracks.length);
            
            if (audioTracks.length === 0) {
                throw new Error('오디오 트랙을 찾을 수 없습니다. 마이크가 켜져 있는지 확인해주세요.');
            }
            
            // 오디오 트랙 상태 확인
            audioTracks.forEach((track, index) => {
                console.log(`🎤 오디오 트랙 ${index}:`, {
                    enabled: track.enabled,
                    muted: track.muted,
                    readyState: track.readyState,
                    label: track.label
                });
            });
            
            // 활성화된 오디오 트랙이 있는지 확인
            const activeAudioTracks = audioTracks.filter(track => track.enabled && track.readyState === 'live');
            if (activeAudioTracks.length === 0) {
                throw new Error('활성화된 오디오 트랙이 없습니다. 마이크를 켜주세요.');
            }
            
            // OpenVidu MediaStream으로 직접 녹화 시작
            await this.startDirectStreamRecording(publisherStream);
            
            console.log('✅ OpenVidu 음성 녹화 시작 완료');
            
        } catch (error) {
            console.error('❌ 녹화 시작 실패:', error);
            throw error;
        }
    }

    async startDirectStreamRecording(mediaStream) {
        try {
            console.log('🎤 OpenVidu MediaStream 직접 녹화 시작');
            
            // MediaRecorder 지원 확인
            if (!window.MediaRecorder) {
                throw new Error('브라우저가 MediaRecorder를 지원하지 않습니다.');
            }
            
            // 오디오 전용 스트림 생성 (비디오 트랙 제거)
            const audioOnlyStream = new MediaStream();
            mediaStream.getAudioTracks().forEach(track => {
                audioOnlyStream.addTrack(track);
            });
            
            // 최적의 MIME 타입 선택
            const supportedTypes = [
                'audio/webm;codecs=opus',
                'audio/webm',
                'audio/ogg;codecs=opus',
                'audio/mp4'
            ];
            
            let selectedMimeType = 'audio/webm'; // 기본값
            for (const type of supportedTypes) {
                if (MediaRecorder.isTypeSupported(type)) {
                    selectedMimeType = type;
                    console.log('✅ 선택된 MIME 타입:', type);
                    break;
                }
            }
            
            // MediaRecorder 생성
            const options = {
                mimeType: selectedMimeType,
                audioBitsPerSecond: 128000 // 고품질 오디오
            };
            
            this.mediaRecorder = new MediaRecorder(audioOnlyStream, options);
            this.recordedChunks = [];
            this.audioLevelMonitor = null;
            
            console.log('📊 MediaRecorder 생성:', this.mediaRecorder.mimeType);
            
            // 실시간 오디오 레벨 모니터링 시작
            this.startAudioLevelMonitoring(audioOnlyStream);
            
            // 녹화 이벤트 핸들러 설정
            this.setupRecordingEventHandlers();
            
            // 녹화 시작
            this.mediaRecorder.start(1000); // 1초마다 데이터 수집
            
            // 상태 업데이트
            this.isRecording = true;
            this.currentRecording = {
                recordingId: 'openvidu_' + Date.now(),
                type: 'openvidu_direct',
                startTime: Date.now(),
                mimeType: selectedMimeType
            };
            this.recordingDuration = 0;
            
            this.updateRecordingUI();
            this.startRecordingTimer();
            
            this.addChatMessage('시스템', '🎵 음성 녹화 시작됨 (OpenVidu 스트림 직접 녹화)');
            
            console.log('✅ OpenVidu 스트림 직접 녹화 시작 완료');
            
        } catch (error) {
            console.error('❌ 직접 스트림 녹화 시작 실패:', error);
            throw error;
        }
    }

    startAudioLevelMonitoring(mediaStream) {
        try {
            // Web Audio API로 실시간 오디오 레벨 분석
            const audioContext = new (window.AudioContext || window.webkitAudioContext)();
            const analyser = audioContext.createAnalyser();
            const source = audioContext.createMediaStreamSource(mediaStream);
            
            source.connect(analyser);
            analyser.fftSize = 256;
            analyser.smoothingTimeConstant = 0.3;
            
            const bufferLength = analyser.frequencyBinCount;
            const dataArray = new Uint8Array(bufferLength);
            
            let silentFrames = 0;
            let lastAudioLevel = 0;
            const maxSilentFrames = 20; // 5초 (250ms * 20)
            
            this.audioLevelMonitor = () => {
                if (!this.isRecording) return;
                
                analyser.getByteFrequencyData(dataArray);
                
                // 평균 오디오 레벨 계산
                let sum = 0;
                for (let i = 0; i < bufferLength; i++) {
                    sum += dataArray[i];
                }
                const audioLevel = sum / bufferLength;
                
                // 오디오 레벨 UI 업데이트 (향후 구현)
                this.updateAudioLevelUI(audioLevel);
                
                // 무음 감지
                if (audioLevel < 2) {
                    silentFrames++;
                    if (silentFrames === maxSilentFrames) {
                        console.warn('⚠️ 5초간 무음 감지됨. 마이크 상태를 확인해주세요.');
                        this.showSilenceWarning();
                    }
                } else {
                    silentFrames = 0;
                    if (audioLevel > 10 && Math.abs(audioLevel - lastAudioLevel) > 5) {
                        console.log(`🎤 오디오 활동: ${audioLevel.toFixed(1)} (정상 녹화 중)`);
                    }
                }
                
                lastAudioLevel = audioLevel;
                
                // 250ms마다 체크
                if (this.isRecording) {
                    setTimeout(this.audioLevelMonitor, 250);
                }
            };
            
            // 모니터링 시작
            this.audioLevelMonitor();
            
            console.log('📊 실시간 오디오 모니터링 시작');
            
        } catch (error) {
            console.warn('⚠️ 오디오 레벨 모니터링 실패:', error.message);
            // 모니터링 실패해도 녹화는 계속 진행
        }
    }

    setupRecordingEventHandlers() {
        // 데이터 수신 이벤트
        this.mediaRecorder.ondataavailable = (event) => {
            if (event.data.size > 0) {
                this.recordedChunks.push(event.data);
                console.log(`📦 청크 수신: ${event.data.size} 바이트 (총 ${this.recordedChunks.length}개)`);
            }
        };

        // 녹화 시작 이벤트
        this.mediaRecorder.onstart = () => {
            console.log('🔴 MediaRecorder 시작됨');
        };

        // 녹화 완료 이벤트
        this.mediaRecorder.onstop = () => {
            console.log('⏹️ MediaRecorder 중단됨');
            this.handleDirectRecordingComplete();
        };

        // 오류 이벤트
        this.mediaRecorder.onerror = (event) => {
            console.error('❌ MediaRecorder 오류:', event.error);
            this.addChatMessage('시스템', '❌ 녹화 중 오류 발생: ' + event.error.message);
        };
    }

    async stopRecording() {
        try {
            console.log('⏹️ 음성 녹화 중단 요청');
            
            if (!this.isRecording || !this.mediaRecorder) {
                console.warn('녹화가 진행 중이지 않습니다.');
                return;
            }
            
            // MediaRecorder 중단
            if (this.mediaRecorder.state === 'recording') {
                this.mediaRecorder.stop();
            }
            
            // 오디오 모니터링 중단
            this.audioLevelMonitor = null;
            
            console.log('✅ 음성 녹화 중단 완료');
            
        } catch (error) {
            console.error('❌ 녹화 중단 실패:', error);
            throw error;
        }
    }

    handleDirectRecordingComplete() {
        console.log('📁 OpenVidu 직접 녹화 완료 처리 시작');
        console.log('📊 수집된 청크 수:', this.recordedChunks.length);
        
        // 청크 상태 분석
        let totalSize = 0;
        this.recordedChunks.forEach((chunk, index) => {
            console.log(`📦 청크 ${index + 1}: ${chunk.size} 바이트, 타입: ${chunk.type}`);
            totalSize += chunk.size;
        });
        
        console.log('📈 전체 데이터 크기:', totalSize, '바이트');

        // 데이터 유효성 검증
        if (this.recordedChunks.length === 0 || totalSize === 0) {
            console.error('❌ 녹화 데이터가 없습니다.');
            this.addChatMessage('시스템', '❌ 녹화 실패: 데이터가 수집되지 않았습니다. 마이크 설정을 확인해주세요.');
            this.resetRecordingState();
            return;
        }

        // 최소 데이터 크기 검증
        if (totalSize < 1024) { // 1KB 미만
            console.warn('⚠️ 녹화 데이터가 매우 작습니다:', totalSize, '바이트');
        }

        const mimeType = this.currentRecording?.mimeType || 'audio/webm';
        console.log('🎵 사용할 MIME 타입:', mimeType);

        try {
            // 오디오 블롭 생성
            const blob = new Blob(this.recordedChunks, { type: mimeType });
            console.log('✅ 오디오 블롭 생성:', blob.size, '바이트, 타입:', blob.type);
            
            if (blob.size === 0) {
                throw new Error('생성된 오디오 블롭의 크기가 0입니다.');
            }
            
            // 블롭 URL 생성
            const blobUrl = URL.createObjectURL(blob);

            // 파일 확장자 결정
            let fileExtension = '.webm';
            if (mimeType.includes('mp4')) fileExtension = '.mp4';
            else if (mimeType.includes('ogg')) fileExtension = '.ogg';
            else if (mimeType.includes('wav')) fileExtension = '.wav';

            // 녹화 정보 객체 생성
            const now = new Date();
            const recordingInfo = {
                recordingId: this.currentRecording.recordingId,
                name: `OpenVidu_음성녹화_${now.getFullYear()}${(now.getMonth()+1).toString().padStart(2,'0')}${now.getDate().toString().padStart(2,'0')}_${now.getHours().toString().padStart(2,'0')}${now.getMinutes().toString().padStart(2,'0')}${fileExtension}`,
                duration: this.recordingDuration,
                size: blob.size,
                url: blobUrl,
                blob: blob,
                type: 'openvidu_direct',
                status: 'ready',
                mimeType: mimeType,
                quality: totalSize > 100000 ? 'high' : totalSize > 10000 ? 'medium' : 'low'
            };

            console.log('📋 녹화 정보 생성 완료:', recordingInfo);

            // 녹화 목록에 추가
            this.recordings.push(recordingInfo);
            this.updateRecordingList();

            // 상태 초기화
            this.resetRecordingState();

            // 성공 메시지
            const sizeKB = (blob.size / 1024).toFixed(1);
            this.addChatMessage('시스템', `✅ 음성 녹화 완료! (${this.recordingDuration}초, ${sizeKB}KB, OpenVidu 직접 녹화)`);
            
            console.log('🎉 OpenVidu 직접 녹화 완료 처리 성공!');
            
        } catch (error) {
            console.error('❌ 녹화 완료 처리 실패:', error);
            this.addChatMessage('시스템', '❌ 녹화 파일 생성 실패: ' + error.message);
            this.resetRecordingState();
        }
    }

    resetRecordingState() {
        // 녹화 상태 초기화
        this.isRecording = false;
        this.currentRecording = null;
        this.mediaRecorder = null;
        this.recordedChunks = [];
        this.audioLevelMonitor = null;
        
        // 타이머 중단
        this.stopRecordingTimer();
        
        // UI 업데이트
        this.updateRecordingUI();
        
        console.log('🔄 녹화 상태 초기화 완료');
    }

    startRecordingTimer() {
        this.recordingTimer = setInterval(() => {
            this.recordingDuration++;
            this.updateRecordingDuration();
        }, 1000);
    }

    stopRecordingTimer() {
        if (this.recordingTimer) {
            clearInterval(this.recordingTimer);
            this.recordingTimer = null;
        }
        this.recordingDuration = 0;
    }

    updateRecordingUI() {
        if (this.isRecording) {
            this.toggleRecordingBtn.textContent = '⏹️ 중단';
            this.toggleRecordingBtn.classList.add('recording-active');
            this.recordingIndicator.textContent = '🔴';
            this.recordingStatusText.textContent = '녹화 중...';
            this.recordingDurationElement.style.display = 'inline';
        } else {
            this.toggleRecordingBtn.textContent = '🎵 녹화';
            this.toggleRecordingBtn.classList.remove('recording-active');
            this.recordingIndicator.textContent = '⚫';
            this.recordingStatusText.textContent = '녹화 대기 중';
            this.recordingDurationElement.style.display = 'none';
        }
    }

    updateRecordingDuration() {
        const minutes = Math.floor(this.recordingDuration / 60);
        const seconds = this.recordingDuration % 60;
        this.recordingDurationElement.textContent = 
            `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
    }

    loadRecordingList() {
        // 브라우저 기반 녹화만 지원하므로 로컬 목록만 사용
        console.log('📋 로컬 녹화 목록 업데이트');
        this.updateRecordingList();
    }

    updateRecordingList() {
        this.recordingList.innerHTML = '';
        
        if (this.recordings.length === 0) {
            const emptyMessage = document.createElement('div');
            emptyMessage.className = 'empty-recordings';
            emptyMessage.innerHTML = `
                <div>📁 아직 녹화된 파일이 없습니다</div>
                <small>화상통화 중 🎵 녹화 버튼을 눌러서 음성을 녹화해보세요</small>
            `;
            this.recordingList.appendChild(emptyMessage);
            return;
        }

        this.recordings.forEach(recording => {
            const recordingItem = document.createElement('div');
            recordingItem.className = 'recording-item';
            
            const duration = Math.floor(recording.duration);
            const sizeKB = recording.size ? (recording.size / 1024).toFixed(1) + 'KB' : 'N/A';
            const status = recording.status;
            const quality = recording.quality || 'medium';
            
            // 품질 표시 이모지
            const qualityEmoji = quality === 'high' ? '🔊' : quality === 'medium' ? '🔉' : '🔈';
            
            recordingItem.innerHTML = `
                <div class="recording-info">
                    <div class="recording-name">🎵 ${recording.name}</div>
                    <div class="recording-details">
                        ${qualityEmoji} ${duration}초 • ${sizeKB} • OpenVidu 직접 녹화
                    </div>
                </div>
                <div class="recording-actions">
                    ${status === 'ready' ? 
                        `<button onclick="videoCallManager.downloadRecording('${recording.recordingId}')" class="download-btn">📥 다운로드</button>` +
                        `<button onclick="videoCallManager.playRecording('${recording.recordingId}')" class="play-btn">▶️ 재생</button>` :
                        '<span class="processing">⏳ 처리 중...</span>'
                    }
                </div>
            `;
            
            this.recordingList.appendChild(recordingItem);
        });
    }

    async downloadRecording(recordingId) {
        try {
            console.log('📥 녹화 파일 다운로드:', recordingId);
            
            // 녹화 정보 찾기
            const recording = this.recordings.find(r => r.recordingId === recordingId);
            
            if (!recording) {
                throw new Error('녹화 파일을 찾을 수 없습니다.');
            }
            
            if (recording.status !== 'ready') {
                throw new Error('녹화 파일이 아직 준비되지 않았습니다.');
            }
            
            // OpenVidu 직접 녹화 파일 다운로드
            console.log('📁 다운로드 시작:', recording.name);
            
            const link = document.createElement('a');
            link.href = recording.url;
            link.download = recording.name;
            link.style.display = 'none';
            
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            
            console.log('✅ 다운로드 완료:', recording.name);
            this.addChatMessage('시스템', `📥 "${recording.name}" 다운로드 완료`);
            
        } catch (error) {
            console.error('❌ 다운로드 오류:', error);
            alert('파일 다운로드에 실패했습니다: ' + error.message);
        }
    }

    playRecording(recordingId) {
        try {
            console.log('▶️ 녹화 파일 재생:', recordingId);
            
            // 녹화 정보 찾기
            const recording = this.recordings.find(r => r.recordingId === recordingId);
            
            if (!recording) {
                throw new Error('녹화 파일을 찾을 수 없습니다.');
            }
            
            if (recording.status !== 'ready') {
                throw new Error('녹화 파일이 아직 준비되지 않았습니다.');
            }
            
            // 오디오 플레이어 생성 및 재생
            this.createAudioPlayer(recording);
            
        } catch (error) {
            console.error('❌ 재생 오류:', error);
            alert('파일 재생에 실패했습니다: ' + error.message);
        }
    }

    createAudioPlayer(recording) {
        // 기존 플레이어 제거
        const existingPlayer = document.getElementById('audioPlayer');
        if (existingPlayer) {
            existingPlayer.remove();
        }
        
        // 새 오디오 플레이어 생성
        const audioPlayer = document.createElement('audio');
        audioPlayer.id = 'audioPlayer';
        audioPlayer.controls = true;
        audioPlayer.style.cssText = `
            width: 100%;
            margin: 10px 0;
            border-radius: 8px;
            background: #f5f5f5;
        `;
        
        audioPlayer.src = recording.url;
        
        // 재생 컨트롤 패널에 추가
        const recordingPanel = document.querySelector('.recording-panel') || this.recordingList.parentElement;
        recordingPanel.insertBefore(audioPlayer, this.recordingList);
        
        // 플레이어 이벤트 리스너
        audioPlayer.addEventListener('play', () => {
            console.log('▶️ 재생 시작:', recording.name);
            this.addChatMessage('시스템', `▶️ "${recording.name}" 재생 시작`);
        });
        
        audioPlayer.addEventListener('ended', () => {
            console.log('⏹️ 재생 완료:', recording.name);
        });
        
        audioPlayer.addEventListener('error', (e) => {
            console.error('❌ 재생 오류:', e);
            this.addChatMessage('시스템', '❌ 오디오 재생 오류');
        });
        
        // 자동 재생 시작
        audioPlayer.play().catch(error => {
            console.warn('자동 재생 실패 (사용자 상호작용 필요):', error.message);
        });
    }

    // UI 관련 새로운 메서드들
    updateAudioLevelUI(audioLevel) {
        // 녹화 버튼 옆에 오디오 레벨 표시 (향후 구현을 위한 플레이스홀더)
        if (audioLevel > 20) {
            // 높은 오디오 레벨
            this.recordingIndicator.textContent = '🔴';
        } else if (audioLevel > 5) {
            // 중간 오디오 레벨  
            this.recordingIndicator.textContent = '🟡';
        } else {
            // 낮은/무음 오디오 레벨
            this.recordingIndicator.textContent = '⚫';
        }
    }

    showSilenceWarning() {
        // 무음 경고 표시
        const warning = document.createElement('div');
        warning.className = 'silence-warning';
        warning.innerHTML = `
            <div>⚠️ 마이크에서 소리가 감지되지 않습니다</div>
            <small>마이크가 켜져 있고 제대로 작동하는지 확인해주세요</small>
        `;
        warning.style.cssText = `
            background: #fff3cd;
            border: 1px solid #ffeaa7;
            border-radius: 8px;
            padding: 10px;
            margin: 10px 0;
            color: #856404;
            text-align: center;
            animation: fadeIn 0.3s ease;
        `;
        
        // 녹화 패널에 경고 추가
        const recordingPanel = document.querySelector('.recording-panel');
        if (recordingPanel) {
            recordingPanel.appendChild(warning);
            
            // 5초 후 경고 제거
            setTimeout(() => {
                if (warning.parentNode) {
                    warning.remove();
                }
            }, 5000);
        }
        
        console.log('⚠️ 무음 경고 표시됨');
    }
}

// 전역 참조를 위한 변수
let videoCallManager;

// 페이지 로드 시 화상통화 매니저 초기화
document.addEventListener('DOMContentLoaded', () => {
    videoCallManager = new VideoCallManager();
});