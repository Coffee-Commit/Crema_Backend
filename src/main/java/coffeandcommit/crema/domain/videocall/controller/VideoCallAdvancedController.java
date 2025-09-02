package coffeandcommit.crema.domain.videocall.controller;

import coffeandcommit.crema.domain.videocall.dto.request.ChatHistorySaveRequest;
import coffeandcommit.crema.domain.videocall.dto.response.ChatHistoryResponse;
import coffeandcommit.crema.domain.videocall.dto.response.QuickJoinResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionConfigResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionStatusResponse;
import coffeandcommit.crema.domain.videocall.service.ChatService;
import coffeandcommit.crema.domain.videocall.service.VideoCallService;
import coffeandcommit.crema.global.common.exception.response.ApiResponse;
import io.openvidu.java.client.Session;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/video-call-advanced")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "화상통화", description = "OpenVidu 화상통화 상세기능 API")
public class VideoCallAdvancedController {

    private final VideoCallService videoCallService;
    private final ChatService chatService;

    @PostMapping("/quick-join")
    public ApiResponse<QuickJoinResponse> quickJoin(
            @RequestParam String sessionName,
            @RequestParam String username,
            @RequestParam(defaultValue = "false") Boolean autoCreateSession) {
        QuickJoinResponse response = videoCallService.quickJoin(sessionName, username, autoCreateSession);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/config")
    public ApiResponse<SessionConfigResponse> getFrontendConfig() {
        SessionConfigResponse response = videoCallService.getFrontendConfig();
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/session/{sessionId}/status")
    public ApiResponse<SessionStatusResponse> getSessionStatus(@PathVariable String sessionId) {
        SessionStatusResponse response = videoCallService.getSessionStatus(sessionId);
        return ApiResponse.onSuccess(response);
    }

    @PostMapping("/session/{sessionId}/refresh-token")
    public ApiResponse<QuickJoinResponse> refreshToken(
            @PathVariable String sessionId,
            @RequestParam String username) {
        QuickJoinResponse response = videoCallService.refreshToken(sessionId, username);
        return ApiResponse.onSuccess(response);
    }

    @PostMapping("/session/{sessionId}/reconnect")
    public ApiResponse<QuickJoinResponse> autoReconnect(
            @PathVariable String sessionId,
            @RequestParam String username,
            @RequestParam(required = false) String lastConnectionId) {
        QuickJoinResponse response = videoCallService.autoReconnect(sessionId, username, lastConnectionId);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/openvidu/status")
    public ApiResponse<List<Session>> getOpenViduStatus() {
        List<Session> response = videoCallService.getOpenViduStatus();
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "채팅 기록 저장", description = "세션의 채팅 기록을 저장합니다.")
    @PostMapping("/sessions/{sessionId}/chat/save-history")
    public ApiResponse<Void> saveChatHistory(
            @PathVariable String sessionId,
            @RequestBody ChatHistorySaveRequest request) {
        chatService.saveChatHistory(sessionId, request);
        return ApiResponse.onSuccess();
    }

    @Operation(summary = "채팅 기록 조회", description = "세션의 채팅 기록을 조회합니다.")
    @GetMapping("/sessions/{sessionId}/chat/history")
    public ApiResponse<ChatHistoryResponse> getChatHistory(@PathVariable String sessionId) {
        ChatHistoryResponse response = chatService.getChatHistory(sessionId);
        return ApiResponse.onSuccess(response);
    }

}
