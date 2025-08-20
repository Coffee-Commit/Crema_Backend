// 화상통화 사이드바 JavaScript

class VideoCallSidebar {
    constructor() {
        // 세션 데이터
        this.sessionData = null;
        
        // 채팅 관련
        this.chatMessages = [];
        
        // 파일 관련
        this.uploadedFiles = [];
        
        // UI 요소들
        this.chatTab = null;
        this.filesTab = null;
        this.chatPanel = null;
        this.filesPanel = null;
        
        this.initializeApp();
    }
    
    async initializeApp() {
        try {
            this.initializeElements();
            this.attachEventListeners();
            this.loadSessionData();
            this.setupWindowCommunication();
            await this.loadChatHistory();
            await this.loadFiles();
            this.showToast('사이드바가 준비되었습니다');
        } catch (error) {
            console.error('사이드바 초기화 실패:', error);
            this.showToast('초기화에 실패했습니다: ' + error.message);
        }
    }
    
    initializeElements() {
        // 탭 관련
        this.chatTab = document.getElementById('chatTab');
        this.filesTab = document.getElementById('filesTab');
        this.chatPanel = document.getElementById('chatPanel');
        this.filesPanel = document.getElementById('filesPanel');
        
        // 채팅 관련
        this.participantsList = document.getElementById('participantsList');
        this.chatMessages = document.getElementById('chatMessages');
        this.chatInput = document.getElementById('chatInput');
        this.sendChatBtn = document.getElementById('sendChatBtn');
        
        // 파일 관련
        this.uploadArea = document.getElementById('uploadArea');
        this.fileInput = document.getElementById('fileInput');
        this.uploadBtn = document.getElementById('uploadBtn');
        this.uploadProgress = document.getElementById('uploadProgress');
        this.progressFill = document.getElementById('progressFill');
        this.progressText = document.getElementById('progressText');
        this.filesList = document.getElementById('filesList');
        this.fileViewer = document.getElementById('fileViewer');
        this.viewerTitle = document.getElementById('viewerTitle');
        this.viewerContent = document.getElementById('viewerContent');
        this.viewerCloseBtn = document.getElementById('viewerCloseBtn');
        
        // 기타
        this.closeBtn = document.getElementById('closeBtn');
        this.loadingOverlay = document.getElementById('loadingOverlay');
        this.toast = document.getElementById('toast');
        this.toastMessage = document.getElementById('toastMessage');
    }
    
    attachEventListeners() {
        // 탭 이벤트
        this.chatTab.addEventListener('click', () => this.switchTab('chat'));
        this.filesTab.addEventListener('click', () => this.switchTab('files'));
        
        // 채팅 이벤트
        this.sendChatBtn.addEventListener('click', () => this.sendChatMessage());
        this.chatInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.sendChatMessage();
            }
        });
        
        // 파일 업로드 이벤트
        this.uploadBtn.addEventListener('click', () => this.fileInput.click());
        this.fileInput.addEventListener('change', (e) => this.handleFileSelect(e.target.files));
        
        // 드래그 앤 드롭 이벤트
        this.uploadArea.addEventListener('dragover', (e) => {
            e.preventDefault();
            this.uploadArea.classList.add('dragover');
        });
        
        this.uploadArea.addEventListener('dragleave', () => {
            this.uploadArea.classList.remove('dragover');
        });
        
        this.uploadArea.addEventListener('drop', (e) => {
            e.preventDefault();
            this.uploadArea.classList.remove('dragover');
            this.handleFileSelect(e.dataTransfer.files);
        });
        
        // 파일 뷰어 이벤트
        this.viewerCloseBtn.addEventListener('click', () => this.closeFileViewer());
        this.fileViewer.addEventListener('click', (e) => {
            if (e.target === this.fileViewer) {
                this.closeFileViewer();
            }
        });
        
        // 창 닫기 이벤트
        this.closeBtn.addEventListener('click', () => window.close());
        
        // ESC 키 이벤트
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                if (this.fileViewer.style.display !== 'none') {
                    this.closeFileViewer();
                }
            }
        });
    }
    
    loadSessionData() {
        const urlParams = new URLSearchParams(window.location.search);
        this.sessionData = {
            sessionId: urlParams.get('sessionId'),
            username: urlParams.get('username')
        };
        
        if (!this.sessionData.sessionId || !this.sessionData.username) {
            throw new Error('세션 정보가 올바르지 않습니다.');
        }
        
        // 참가자 목록에 자신 추가
        this.addParticipant(this.sessionData.username, true);
    }
    
    setupWindowCommunication() {
        // 부모 창에 세션 정보 요청
        if (window.opener) {
            window.opener.postMessage({
                type: 'REQUEST_SESSION_INFO'
            }, window.location.origin);
        }
        
        // 부모 창으로부터 메시지 수신
        window.addEventListener('message', (event) => {
            if (event.origin !== window.location.origin) return;
            
            const { type, data } = event.data;
            
            switch (type) {
                case 'SESSION_INFO':
                    this.sessionData = { ...this.sessionData, ...data };
                    break;
                case 'CHAT_MESSAGE':
                    this.displayChatMessage(data.username, data.message, false);
                    break;
                case 'PARTICIPANT_JOINED':
                    this.addParticipant(data.username, false);
                    break;
                case 'PARTICIPANT_LEFT':
                    this.removeParticipant(data.username);
                    break;
            }
        });
    }
    
    // 탭 전환
    switchTab(tabName) {
        // 탭 버튼 활성화
        document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
        document.querySelectorAll('.tab-panel').forEach(panel => panel.classList.remove('active'));
        
        if (tabName === 'chat') {
            this.chatTab.classList.add('active');
            this.chatPanel.classList.add('active');
        } else if (tabName === 'files') {
            this.filesTab.classList.add('active');
            this.filesPanel.classList.add('active');
        }
    }
    
    // 참가자 관리
    addParticipant(username, isOwn = false) {
        const existingParticipant = this.participantsList.querySelector(`[data-username="${username}"]`);
        if (existingParticipant) return;
        
        const participantEl = document.createElement('div');
        participantEl.className = 'participant-item';
        participantEl.setAttribute('data-username', username);
        
        participantEl.innerHTML = `
            <div class="participant-avatar">${username.charAt(0).toUpperCase()}</div>
            <div class="participant-info">
                <div class="participant-name">${username}${isOwn ? ' (나)' : ''}</div>
                <div class="participant-status online">온라인</div>
            </div>
        `;
        
        this.participantsList.appendChild(participantEl);
    }
    
    removeParticipant(username) {
        const participantEl = this.participantsList.querySelector(`[data-username="${username}"]`);
        if (participantEl) {
            participantEl.remove();
        }
    }
    
    // 채팅 메시지 전송
    sendChatMessage() {
        const message = this.chatInput.value.trim();
        if (!message) return;
        
        // 메인 창에 메시지 전송 요청
        if (window.opener) {
            window.opener.postMessage({
                type: 'SEND_CHAT',
                data: { message }
            }, window.location.origin);
        }
        
        // 자신의 메시지 즉시 표시
        this.displayChatMessage(this.sessionData.username, message, true);
        
        // 입력창 초기화
        this.chatInput.value = '';
    }
    
    // 채팅 메시지 표시
    displayChatMessage(username, message, isOwn = false) {
        const messageEl = document.createElement('div');
        messageEl.className = `chat-message ${isOwn ? 'own' : ''}`;
        
        const currentTime = new Date().toLocaleTimeString('ko-KR', {
            hour: '2-digit',
            minute: '2-digit'
        });
        
        messageEl.innerHTML = `
            <div class="message-avatar">${username.charAt(0).toUpperCase()}</div>
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
    
    // 채팅 기록 로드
    async loadChatHistory() {
        try {
            const response = await fetch(`/api/chat/sessions/${this.sessionData.sessionId}/messages`);
            if (response.ok) {
                const messages = await response.json();
                messages.forEach(msg => {
                    const isOwn = msg.username === this.sessionData.username;
                    this.displayChatMessage(msg.username, msg.message, isOwn);
                });
            }
        } catch (error) {
            console.error('채팅 기록 로드 실패:', error);
        }
    }
    
    // 파일 선택 처리
    handleFileSelect(files) {
        if (!files || files.length === 0) return;
        
        Array.from(files).forEach(file => {
            this.uploadFile(file);
        });
    }
    
    // 파일 업로드
    async uploadFile(file) {
        // 파일 타입 검증
        const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'application/pdf'];
        if (!allowedTypes.includes(file.type)) {
            this.showToast('지원하지 않는 파일 형식입니다. (이미지 또는 PDF만 가능)');
            return;
        }
        
        // 파일 크기 검증 (10MB)
        if (file.size > 10 * 1024 * 1024) {
            this.showToast('파일 크기는 10MB 이하만 가능합니다.');
            return;
        }
        
        const formData = new FormData();
        formData.append('file', file);
        formData.append('sessionId', this.sessionData.sessionId);
        formData.append('username', this.sessionData.username);
        
        this.showUploadProgress(true);
        
        try {
            const response = await fetch('/api/files/upload', {
                method: 'POST',
                body: formData
            });
            
            if (response.ok) {
                const result = await response.json();
                console.log('업로드 응답:', result); // 디버깅용 로그
                this.addFileToList(result);
                this.showToast('파일이 업로드되었습니다.');
            } else {
                const errorText = await response.text();
                console.error('업로드 실패 응답:', errorText);
                throw new Error('업로드 실패: ' + response.status);
            }
        } catch (error) {
            console.error('파일 업로드 실패:', error);
            this.showToast('파일 업로드에 실패했습니다.');
        } finally {
            this.showUploadProgress(false);
        }
    }
    
    // 업로드 진행률 표시
    showUploadProgress(show) {
        if (show) {
            this.uploadProgress.style.display = 'block';
            // 간단한 진행률 애니메이션
            let progress = 0;
            const interval = setInterval(() => {
                progress += 10;
                this.progressFill.style.width = progress + '%';
                this.progressText.textContent = progress + '%';
                
                if (progress >= 100) {
                    clearInterval(interval);
                }
            }, 100);
        } else {
            setTimeout(() => {
                this.uploadProgress.style.display = 'none';
                this.progressFill.style.width = '0%';
                this.progressText.textContent = '0%';
            }, 500);
        }
    }
    
    // 파일 목록에 추가
    addFileToList(fileData) {
        const fileEl = document.createElement('div');
        fileEl.className = 'file-item';
        fileEl.setAttribute('data-file-id', fileData.id);
        
        // fileType 필드명 확인 및 기본값 설정
        const fileType = fileData.fileType || fileData.type || 'application/octet-stream';
        const isImage = fileType.startsWith('image/');
        const iconClass = isImage ? 'image' : 'pdf';
        const iconText = isImage ? '🖼️' : '📄';
        
        // 파일명과 크기 처리
        const fileName = fileData.originalFileName || fileData.fileName || fileData.name || '파일';
        const fileSize = fileData.fileSize || fileData.size || 0;
        const uploader = fileData.uploader || fileData.username || '알 수 없음';
        
        // 파일 타입별 버튼 설정
        const isImageFile = isImage;
        const viewButtonText = isImageFile ? '보기' : '다운로드';
        const viewButtonIcon = isImageFile ? '👁️' : '⬇️';
        const viewButtonTitle = isImageFile ? '이미지 뷰어로 보기' : 'PDF 다운로드';
        
        fileEl.innerHTML = `
            <div class="file-icon ${iconClass}">${iconText}</div>
            <div class="file-info">
                <div class="file-name" title="${fileName}">${fileName}</div>
                <div class="file-details">
                    <span class="file-size">${this.formatFileSize(fileSize)}</span>
                    <span class="file-uploader">${uploader}</span>
                    <span class="file-type-badge ${iconClass}">${isImageFile ? '이미지' : 'PDF'}</span>
                </div>
            </div>
            <div class="file-actions">
                <button class="file-action-btn primary" onclick="sidebar.viewFile('${fileData.id}')" title="${viewButtonTitle}">
                    <span class="btn-icon">${viewButtonIcon}</span>
                    <span class="btn-text">${viewButtonText}</span>
                </button>
                <button class="file-action-btn secondary" onclick="sidebar.downloadFile('${fileData.id}')" title="파일 다운로드">
                    <span class="btn-icon">⬇️</span>
                </button>
            </div>
        `;
        
        // 파일 아이템 클릭 시 뷰어 열기
        fileEl.addEventListener('click', (e) => {
            if (!e.target.classList.contains('file-action-btn')) {
                this.viewFile(fileData.id);
            }
        });
        
        this.filesList.appendChild(fileEl);
        this.uploadedFiles.push(fileData);
    }
    
    // 파일 목록 로드
    async loadFiles() {
        try {
            const response = await fetch(`/api/files/session/${this.sessionData.sessionId}`);
            if (response.ok) {
                const files = await response.json();
                files.forEach(file => {
                    this.addFileToList(file);
                });
                
                if (files.length === 0) {
                    this.showEmptyFilesState();
                }
            }
        } catch (error) {
            console.error('파일 목록 로드 실패:', error);
            this.showEmptyFilesState();
        }
    }
    
    // 빈 파일 목록 상태 표시
    showEmptyFilesState() {
        if (this.filesList.children.length === 0) {
            this.filesList.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">📁</div>
                    <div class="empty-text">아직 공유된 자료가 없습니다</div>
                    <div class="empty-hint">파일을 업로드하여 공유해보세요</div>
                </div>
            `;
        }
    }
    
    // 파일 보기/다운로드 처리
    async viewFile(fileId) {
        try {
            const fileData = this.uploadedFiles.find(f => f.id === fileId);
            if (!fileData) return;
            
            // 파일명 처리 (fileName 또는 originalFileName 또는 name)
            const fileName = fileData.originalFileName || fileData.fileName || fileData.name || '파일';
            
            // fileType 필드명 확인 및 기본값 설정
            const fileType = fileData.fileType || fileData.type || 'application/octet-stream';
            
            if (fileType.startsWith('image/')) {
                // 이미지 파일: 뷰어에서 바로 보기
                this.viewerTitle.textContent = fileName;
                
                // 로딩 스피너 표시
                this.viewerContent.innerHTML = `
                    <div class="image-loading">
                        <div class="loading-spinner"></div>
                        <p>이미지를 로딩 중...</p>
                    </div>
                `;
                this.fileViewer.style.display = 'flex';
                
                // 이미지 로드
                const img = new Image();
                img.onload = () => {
                    this.viewerContent.innerHTML = `
                        <img src="/api/files/${fileId}/view" alt="${fileName}">
                    `;
                };
                img.onerror = () => {
                    this.viewerContent.innerHTML = `
                        <div class="image-error">
                            <div class="error-icon">❌</div>
                            <p>이미지를 로드할 수 없습니다</p>
                            <button class="retry-btn" onclick="sidebar.viewFile('${fileId}')">다시 시도</button>
                        </div>
                    `;
                };
                img.src = `/api/files/${fileId}/view`;
                
            } else if (fileType === 'application/pdf') {
                // PDF 파일: 다운로드
                this.downloadFile(fileId);
                this.showToast('PDF 파일을 다운로드합니다.');
                
            } else {
                // 기타 파일: 다운로드
                this.downloadFile(fileId);
                this.showToast('파일을 다운로드합니다.');
            }
            
        } catch (error) {
            console.error('파일 처리 실패:', error);
            this.showToast('파일을 처리할 수 없습니다.');
        }
    }
    
    // 파일 뷰어 닫기
    closeFileViewer() {
        this.fileViewer.style.display = 'none';
        this.viewerContent.innerHTML = '';
    }
    
    // 파일 다운로드
    async downloadFile(fileId) {
        try {
            const fileData = this.uploadedFiles.find(f => f.id === fileId);
            if (!fileData) {
                this.showToast('파일 정보를 찾을 수 없습니다.');
                return;
            }
            
            const fileName = fileData.originalFileName || fileData.fileName || fileData.name || '파일';
            
            // 다운로드 시작 알림
            this.showToast('다운로드를 시작합니다...');
            
            // 파일 존재 여부 확인 (HEAD 요청)
            const checkResponse = await fetch(`/api/files/${fileId}/download`, { method: 'HEAD' });
            if (!checkResponse.ok) {
                throw new Error('파일을 찾을 수 없습니다.');
            }
            
            // 다운로드 실행
            const link = document.createElement('a');
            link.href = `/api/files/${fileId}/download`;
            link.download = fileName;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            
            // 다운로드 완료 알림 (약간의 지연 후)
            setTimeout(() => {
                this.showToast('다운로드가 완료되었습니다.');
            }, 1000);
            
        } catch (error) {
            console.error('파일 다운로드 실패:', error);
            this.showToast('파일 다운로드에 실패했습니다: ' + error.message);
        }
    }
    
    // 유틸리티 메소드들
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
    
    showLoading(show) {
        this.loadingOverlay.style.display = show ? 'flex' : 'none';
    }
    
    showToast(message) {
        this.toastMessage.textContent = message;
        this.toast.style.display = 'block';
        
        setTimeout(() => {
            this.toast.style.display = 'none';
        }, 3000);
    }
}

// 전역 사이드바 인스턴스
let sidebar;

// 페이지 로드 시 앱 초기화
document.addEventListener('DOMContentLoaded', () => {
    sidebar = new VideoCallSidebar();
});