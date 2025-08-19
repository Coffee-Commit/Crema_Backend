#!/bin/bash

echo "========================================"
echo "OpenVidu 개발 서버 시작 스크립트"
echo "========================================"

echo
echo "1. Docker가 실행 중인지 확인..."
if ! command -v docker &> /dev/null; then
    echo "❌ Docker가 설치되어 있지 않습니다."
    echo "Docker를 설치한 후 다시 시도해주세요."
    exit 1
fi

if ! docker info &> /dev/null; then
    echo "❌ Docker가 실행 중이 아닙니다."
    echo "Docker를 실행한 후 다시 시도해주세요."
    exit 1
fi
echo "✅ Docker 확인 완료"

echo
echo "2. 기존 OpenVidu 컨테이너 중지 및 제거..."
docker-compose -f docker-compose.openvidu.yml down
echo "✅ 기존 컨테이너 정리 완료"

echo
echo "3. OpenVidu 서버 시작..."
docker-compose -f docker-compose.openvidu.yml up -d

echo
echo "4. OpenVidu 서버 상태 확인 중..."
sleep 10

# 서버 준비 대기
while true; do
    echo "OpenVidu 서버 연결 확인 중..."
    if curl -s http://localhost:25565/openvidu/api/config > /dev/null 2>&1; then
        break
    fi
    echo "서버 준비 중... 5초 후 재시도"
    sleep 5
done

echo
echo "========================================"
echo "✅ OpenVidu 서버가 성공적으로 시작되었습니다!"
echo "========================================"
echo
echo "📍 서버 정보:"
echo "  - HTTP URL: http://localhost:25565"
echo "  - HTTPS URL: https://localhost:25566 (자체 서명 인증서)"
echo "  - Secret: MY_SECRET"
echo
echo "🌐 테스트 페이지:"
echo "  - http://localhost:8081/test-videocall.html"
echo
echo "🔧 관리:"
echo "  - 로그 확인: docker-compose -f docker-compose.openvidu.yml logs -f"
echo "  - 서버 중지: docker-compose -f docker-compose.openvidu.yml down"
echo
echo "Spring Boot 애플리케이션을 시작할 수 있습니다."
echo "========================================"