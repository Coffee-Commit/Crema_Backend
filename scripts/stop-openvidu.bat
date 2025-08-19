@echo off
echo ========================================
echo OpenVidu 개발 서버 중지 스크립트
echo ========================================

echo.
echo OpenVidu 서버 중지 중...
docker-compose -f docker-compose.openvidu.yml down

echo.
echo ✅ OpenVidu 서버가 중지되었습니다.
echo.
echo 추가 정리 옵션:
echo 1. 볼륨까지 삭제하려면: docker-compose -f docker-compose.openvidu.yml down -v
echo 2. 이미지까지 삭제하려면: docker rmi openvidu/openvidu-dev:2.30.0

pause