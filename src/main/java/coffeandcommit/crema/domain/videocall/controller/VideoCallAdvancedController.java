package coffeandcommit.crema.domain.videocall.controller;

import coffeandcommit.crema.domain.videocall.dto.request.QuickJoinRequest;
import coffeandcommit.crema.domain.videocall.dto.response.QuickJoinResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionConfigResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionStatusResponse;
import coffeandcommit.crema.domain.videocall.service.VideoCallAdvancedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 프론트엔드 개발자의 작업을 간소화하기 위한 고급 화상통화 API
 * 복잡한 OpenVidu 설정과 세션 관리를 백엔드에서 처리
 */
@RestController
@RequestMapping("/api/video-call/advanced")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "고급 화상통화", description = "프론트엔드 작업 간소화를 위한 고급 OpenVidu API")
public class VideoCallAdvancedController {

    private final VideoCallAdvancedService videoCallAdvancedService;

    @PostMapping("/quick-join")
    @Operation(
        summary = "원클릭 세션 참가", 
        description = "세션 생성부터 토큰 발급까지 한 번에 처리하여 프론트엔드 작업을 간소화합니다."
    )
    public ResponseEntity<QuickJoinResponse> quickJoin(
            @Valid @RequestBody QuickJoinRequest request) {
        
        log.info("원클릭 참가 요청: sessionName={}, username={}", 
                request.getSessionName(), request.getUsername());
        
        QuickJoinResponse response = videoCallAdvancedService.quickJoin(
                request.getSessionName(), 
                request.getUsername(),
                request.getAutoCreateSession()
        );
        
        log.info("원클릭 참가 완료: sessionId={}, username={}", 
                response.getSessionId(), response.getUsername());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/config")
    @Operation(
        summary = "프론트엔드 설정 정보", 
        description = "OpenVidu 연결에 필요한 모든 설정 정보를 프론트엔드에 제공합니다."
    )
    public ResponseEntity<SessionConfigResponse> getConfig() {
        
        log.info("프론트엔드 설정 정보 요청");
        
        SessionConfigResponse config = videoCallAdvancedService.getFrontendConfig();
        
        return ResponseEntity.ok(config);
    }

    @GetMapping("/sessions/{sessionId}/status")
    @Operation(
        summary = "실시간 세션 상태", 
        description = "세션의 실시간 상태와 참가자 정보를 제공합니다."
    )
    public ResponseEntity<SessionStatusResponse> getSessionStatus(
            @PathVariable String sessionId) {
        
        log.info("세션 상태 조회: sessionId={}", sessionId);
        
        SessionStatusResponse status = videoCallAdvancedService.getSessionStatus(sessionId);
        
        return ResponseEntity.ok(status);
    }

    @PostMapping("/sessions/{sessionId}/refresh-token")
    @Operation(
        summary = "토큰 갱신", 
        description = "세션 토큰을 자동으로 갱신합니다."
    )
    public ResponseEntity<QuickJoinResponse> refreshToken(
            @PathVariable String sessionId,
            @RequestParam String username) {
        
        log.info("토큰 갱신 요청: sessionId={}, username={}", sessionId, username);
        
        QuickJoinResponse response = videoCallAdvancedService.refreshToken(sessionId, username);
        
        log.info("토큰 갱신 완료: sessionId={}", sessionId);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{sessionId}/auto-reconnect")
    @Operation(
        summary = "자동 재연결", 
        description = "네트워크 오류 시 세션 자동 재연결을 처리합니다."
    )
    public ResponseEntity<QuickJoinResponse> autoReconnect(
            @PathVariable String sessionId,
            @RequestParam String username,
            @RequestParam String lastConnectionId) {
        
        log.info("자동 재연결 요청: sessionId={}, username={}, lastConnectionId={}", 
                sessionId, username, lastConnectionId);
        
        QuickJoinResponse response = videoCallAdvancedService.autoReconnect(
                sessionId, username, lastConnectionId
        );
        
        log.info("자동 재연결 완료: sessionId={}", sessionId);
        
        return ResponseEntity.ok(response);
    }
}