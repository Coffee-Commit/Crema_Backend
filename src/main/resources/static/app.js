// 메인 페이지 JavaScript

class VideoCallApp {
    constructor() {
        this.sessionData = null;
        this.initializeElements();
        this.attachEventListeners();
    }

    initializeElements() {
        this.sessionNameInput = document.getElementById('sessionName');
        this.createSessionBtn = document.getElementById('createSessionBtn');
        this.joinSessionIdInput = document.getElementById('joinSessionId');
        this.usernameInput = document.getElementById('username');
        this.joinSessionBtn = document.getElementById('joinSessionBtn');
        this.sessionInfo = document.getElementById('sessionInfo');
        this.sessionDetails = document.getElementById('sessionDetails');
        this.startVideoCallBtn = document.getElementById('startVideoCallBtn');
        this.messageDiv = document.getElementById('message');
    }

    attachEventListeners() {
        this.createSessionBtn.addEventListener('click', () => this.createSession());
        this.joinSessionBtn.addEventListener('click', () => this.joinSession());
        this.startVideoCallBtn.addEventListener('click', () => this.startVideoCall());

        // Enter 키 이벤트 처리
        this.sessionNameInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.createSession();
        });

        this.usernameInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.joinSession();
        });
    }

    async createSession() {
        const sessionName = this.sessionNameInput.value.trim();
        
        if (!sessionName) {
            this.showMessage('세션 이름을 입력해주세요.', 'error');
            return;
        }

        try {
            this.showMessage('세션을 생성하는 중...', 'info');
            
            const response = await fetch('/api/video-call/sessions', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ sessionName })
            });

            if (!response.ok) {
                throw new Error('세션 생성에 실패했습니다.');
            }

            const sessionData = await response.json();
            
            // 세션 생성 후 자동으로 참가
            const username = `사용자${Math.floor(Math.random() * 1000)}`;
            await this.autoJoinSession(sessionData.sessionId, username);
            
            this.showMessage('세션이 성공적으로 생성되었습니다!', 'success');
            
        } catch (error) {
            console.error('세션 생성 오류:', error);
            this.showMessage('세션 생성에 실패했습니다: ' + error.message, 'error');
        }
    }

    async joinSession() {
        const sessionId = this.joinSessionIdInput.value.trim();
        const username = this.usernameInput.value.trim();
        
        if (!sessionId || !username) {
            this.showMessage('세션 ID와 사용자명을 모두 입력해주세요.', 'error');
            return;
        }

        try {
            this.showMessage('세션에 참가하는 중...', 'info');
            
            const response = await fetch(`/api/video-call/sessions/${sessionId}/join`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ username })
            });

            if (!response.ok) {
                throw new Error('세션 참가에 실패했습니다.');
            }

            const joinData = await response.json();
            
            // 세션 정보 저장
            this.sessionData = {
                sessionId: joinData.sessionId,
                username: joinData.username,
                token: joinData.token
            };
            
            this.showSessionInfo(this.sessionData);
            this.showMessage('세션에 성공적으로 참가했습니다!', 'success');
            
        } catch (error) {
            console.error('세션 참가 오류:', error);
            this.showMessage('세션 참가에 실패했습니다: ' + error.message, 'error');
        }
    }

    async autoJoinSession(sessionId, username) {
        try {
            const response = await fetch(`/api/video-call/sessions/${sessionId}/join`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ username })
            });

            if (!response.ok) {
                throw new Error('세션 참가에 실패했습니다.');
            }

            const joinData = await response.json();
            
            // 세션 정보 저장
            this.sessionData = {
                sessionId: joinData.sessionId,
                username: joinData.username,
                token: joinData.token
            };
            
            this.showSessionInfo(this.sessionData);
            
        } catch (error) {
            console.error('자동 세션 참가 오류:', error);
            throw error;
        }
    }

    showSessionInfo(data) {
        this.sessionInfo.style.display = 'block';
        this.sessionDetails.innerHTML = `
            <p><strong>세션 ID:</strong> ${data.sessionId}</p>
            ${data.sessionName ? `<p><strong>세션 이름:</strong> ${data.sessionName}</p>` : ''}
            ${data.username ? `<p><strong>사용자명:</strong> ${data.username}</p>` : ''}
            <p><strong>상태:</strong> 준비됨</p>
        `;
    }

    startVideoCall() {
        if (!this.sessionData) {
            this.showMessage('세션 정보가 없습니다.', 'error');
            return;
        }

        // 선택된 UI 버전 확인
        const selectedVersion = document.querySelector('input[name="uiVersion"]:checked').value;
        
        // 새로운 화상통화 페이지로 이동 (URL 파라미터로 세션 데이터 전달)
        const params = new URLSearchParams({
            sessionId: this.sessionData.sessionId,
            username: this.sessionData.username,
            token: this.sessionData.token
        });
        
        // 선택된 UI 버전에 따라 다른 페이지로 이동
        let pageUrl;
        switch(selectedVersion) {
            case 'v2':
                pageUrl = 'new-video-call-v2.html';
                break;
            case 'v3':
                pageUrl = 'new-video-call-v3.html';
                break;
            default:
                pageUrl = 'new-video-call.html';
                break;
        }
        
        window.location.href = `/${pageUrl}?${params.toString()}`;
    }

    showMessage(message, type = 'info') {
        this.messageDiv.textContent = message;
        this.messageDiv.className = `message ${type}`;
        
        // 3초 후 메시지 제거
        setTimeout(() => {
            this.messageDiv.textContent = '';
            this.messageDiv.className = 'message';
        }, 3000);
    }
}

// 페이지 로드 시 앱 초기화
document.addEventListener('DOMContentLoaded', () => {
    new VideoCallApp();
});