@echo off
echo ========================================
echo OpenVidu 개발 서버 시작 스크립트
echo ========================================

echo.
echo 1. Docker가 실행 중인지 확인...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker가 설치되어 있지 않거나 실행 중이 아닙니다.
    echo Docker Desktop을 설치하고 실행한 후 다시 시도해주세요.
    pause
    exit /b 1
)
echo ✅ Docker 확인 완료

echo.
echo 2. 기존 OpenVidu 컨테이너 중지 및 제거...
docker-compose -f docker-compose.openvidu.yml down
echo ✅ 기존 컨테이너 정리 완료

echo.
echo 3. OpenVidu 서버 시작...
docker-compose -f docker-compose.openvidu.yml up -d

echo.
echo 4. OpenVidu 서버 상태 확인 중...
timeout /t 10 /nobreak > nul

:check_loop
echo OpenVidu 서버 연결 확인 중...
curl -s http://localhost:25565/openvidu/api/config >nul 2>&1
if %errorlevel% neq 0 (
    echo 서버 준비 중... 5초 후 재시도
    timeout /t 5 /nobreak > nul
    goto check_loop
)

echo.
echo ========================================
echo ✅ OpenVidu 서버가 성공적으로 시작되었습니다!
echo ========================================
echo.
echo 📍 서버 정보:
echo   - HTTP URL: http://localhost:25565
echo   - HTTPS URL: https://localhost:25566 (자체 서명 인증서)
echo   - Secret: MY_SECRET
echo.
echo 🌐 테스트 페이지:
echo   - http://localhost:8081/test-videocall.html
echo.
echo 🔧 관리:
echo   - 로그 확인: docker-compose -f docker-compose.openvidu.yml logs -f
echo   - 서버 중지: docker-compose -f docker-compose.openvidu.yml down
echo.
echo Spring Boot 애플리케이션을 시작할 수 있습니다.
echo ========================================

pause