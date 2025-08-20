package coffeandcommit.crema.domain.videocall.controller;

import coffeandcommit.crema.domain.videocall.dto.request.CreateSessionRequest;
import coffeandcommit.crema.domain.videocall.dto.request.JoinSessionRequest;
import coffeandcommit.crema.domain.videocall.dto.response.JoinSessionResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionResponse;
import coffeandcommit.crema.domain.videocall.entity.VideoSession;
import coffeandcommit.crema.domain.videocall.service.VideoCallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/video-call")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "화상통화", description = "OpenVidu 화상통화 API")
public class VideoCallController {

    private final VideoCallService videoCallService;

    @PostMapping("/sessions")
    @Operation(summary = "세션 생성", description = "새로운 화상통화 세션을 생성합니다.")
    public ResponseEntity<SessionResponse> createSession(
            @Valid @RequestBody CreateSessionRequest request) {
        
        log.info("세션 생성 요청: {}", request.getSessionName());
        
        VideoSession session = videoCallService.createSession(request.getSessionName());
        SessionResponse response = SessionResponse.from(session);
        
        log.info("세션 생성 완료: {}", session.getSessionId());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{sessionId}/join")
    @Operation(summary = "세션 참가", description = "기존 화상통화 세션에 참가합니다.")
    public ResponseEntity<JoinSessionResponse> joinSession(
            @PathVariable String sessionId,
            @Valid @RequestBody JoinSessionRequest request) {
        
        log.info("세션 참가 요청: sessionId={}, username={}", sessionId, request.getUsername());
        
        String token = videoCallService.joinSession(sessionId, request.getUsername());
        
        JoinSessionResponse response = JoinSessionResponse.builder()
                .token(token)
                .sessionId(sessionId)
                .username(request.getUsername())
                .build();
        
        log.info("세션 참가 완료: sessionId={}, username={}", sessionId, request.getUsername());
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/sessions/{sessionId}/leave")
    @Operation(summary = "세션 나가기", description = "화상통화 세션에서 나갑니다.")
    public ResponseEntity<Void> leaveSession(
            @PathVariable String sessionId,
            @RequestParam String connectionId) {
        
        log.info("세션 나가기 요청: sessionId={}, connectionId={}", sessionId, connectionId);
        
        videoCallService.leaveSession(sessionId, connectionId);
        
        log.info("세션 나가기 완료: sessionId={}, connectionId={}", sessionId, connectionId);
        
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "세션 종료", description = "화상통화 세션을 완전히 종료합니다.")
    public ResponseEntity<Void> endSession(@PathVariable String sessionId) {
        
        log.info("세션 종료 요청: sessionId={}", sessionId);
        
        videoCallService.endSession(sessionId);
        
        log.info("세션 종료 완료: sessionId={}", sessionId);
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "세션 정보 조회", description = "화상통화 세션 정보를 조회합니다.")
    public ResponseEntity<SessionResponse> getSession(@PathVariable String sessionId) {
        
        log.info("세션 정보 조회 요청: sessionId={}", sessionId);
        
        VideoSession session = videoCallService.getSession(sessionId);
        SessionResponse response = SessionResponse.from(session);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{sessionId}/screen-share/start")
    @Operation(summary = "화면공유 시작", description = "세션에서 화면공유를 시작합니다.")
    public ResponseEntity<Void> startScreenShare(
            @PathVariable String sessionId,
            @RequestParam String connectionId) {
        
        log.info("화면공유 시작 요청: sessionId={}, connectionId={}", sessionId, connectionId);
        
        videoCallService.startScreenShare(sessionId, connectionId);
        
        log.info("화면공유 시작 완료: sessionId={}, connectionId={}", sessionId, connectionId);
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sessions/{sessionId}/screen-share/stop")
    @Operation(summary = "화면공유 중지", description = "세션에서 화면공유를 중지합니다.")
    public ResponseEntity<Void> stopScreenShare(
            @PathVariable String sessionId,
            @RequestParam String connectionId) {
        
        log.info("화면공유 중지 요청: sessionId={}, connectionId={}", sessionId, connectionId);
        
        videoCallService.stopScreenShare(sessionId, connectionId);
        
        log.info("화면공유 중지 완료: sessionId={}, connectionId={}", sessionId, connectionId);
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sessions/{sessionId}/screen-share/status")
    @Operation(summary = "화면공유 상태 조회", description = "세션의 화면공유 상태를 조회합니다.")
    public ResponseEntity<Boolean> getScreenShareStatus(@PathVariable String sessionId) {
        
        log.info("화면공유 상태 조회 요청: sessionId={}", sessionId);
        
        boolean isScreenSharing = videoCallService.isScreenSharing(sessionId);
        
        log.info("화면공유 상태 조회 완료: sessionId={}, isScreenSharing={}", sessionId, isScreenSharing);
        
        return ResponseEntity.ok(isScreenSharing);
    }
}