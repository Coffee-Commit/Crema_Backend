package coffeandcommit.crema.domain.videocall.controller;

import coffeandcommit.crema.domain.videocall.dto.request.QuickJoinRequest;
import coffeandcommit.crema.domain.videocall.dto.response.QuickJoinResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionJoinResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionStatusResponse;
import coffeandcommit.crema.domain.videocall.service.VideoCallService;
import coffeandcommit.crema.global.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/video-call")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "화상통화", description = "OpenVidu 화상통화 API")
public class VideoCallController {

    private final VideoCallService videoCallService;

    @PostMapping("/quick-join")
    @Operation(
            summary = "세션 참가(자동 세션생성)"
    )
    public ResponseEntity<QuickJoinResponse> quickJoin(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody QuickJoinRequest quickJoinRequest
            ) {
        QuickJoinResponse response = videoCallService.quickJoin();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/config")
    @Operation(summary = "프론트엔드 연결 정보")
    public ResponseEntity<SessionJoinResponse> getSessionConfig() {

        SessionJoinResponse configResponse = videoCallService.getFrontendConfig();

        return ResponseEntity.ok(configResponse);
    }

    @GetMapping("/sessions/{sessionId}/status")
    @Operation(
            summary = "실시간 세션 상태"
    )
    public ResponseEntity<SessionStatusResponse> getSessionStatus(@PathVariable String sessionId){

        SessionStatusResponse sessionStatusResponse = videoCallService.getSessionStatus();

        return ResponseEntity.ok(sessionStatusResponse);
    }

    @PostMapping("/sessions/{sessionId}/refresh-token")
    @Operation(
            summary = "토큰 갱신"
    )
    public ResponseEntity<QuickJoinResponse> refreshSessionToken(
            @PathVariable String sessionId,
            @RequestParam String username){
        QuickJoinResponse response = videoCallService.refreshToken(sessionId, username);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{sessionId}/auto-reconnect")
    @Operation(
            summary = "자동 재연결"
    )
    public ResponseEntity<QuickJoinResponse> autoReconnect(
            @PathVariable String sessionId,
            @RequestParam String username,
            @RequestParam String lastConnectionId){
        QuickJoinResponse quickJoinResponse = videoCallService.autoReconnect(sessionId, username, lastConnectionId);
        return ResponseEntity.ok(quickJoinResponse);
    }

    //관리자용 openvidu 서버 상태 조회 추후구현
//    @GetMapping("/openvidu-status")
//    @Operation(
//            summary = "OpenVidu 서버 상태 확인"
//    )
//    public ResponseEntity<Map<String, Object>> getOpenviduStatus() {
//        Map<String, Object> status = new HashMap<>();
//        try {
//            // OpenVidu 서버 연결 테스트
//            var activeSessions = videoCallService.getOpenViduStatus();
//
//            status.put("connected", true);
//            status.put("activeSessionCount", activeSessions.size());
//            status.put("activeSessions", activeSessions.stream()
//                    .map(session -> Map.of(
//                            "sessionId", session.getSessionId(),
//                            "connectionCount", session.getActiveConnections().size(),
//                            "createdAt", session.createdAt()
//                    ))
//                    .collect(java.util.stream.Collectors.toList()));
//            status.put("message", "OpenVidu 서버 연결 정상");
//
//        } catch (Exception e) {
//            status.put("connected", false);
//            status.put("error", e.getMessage());
//            status.put("message", "OpenVidu 서버 연결 실패");
//            log.error("OpenVidu 상태 확인 실패: {}", e.getMessage());
//        }
//
//        return ResponseEntity.ok(status);
//    }
}
