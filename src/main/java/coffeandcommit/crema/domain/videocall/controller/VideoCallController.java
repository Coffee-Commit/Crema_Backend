package coffeandcommit.crema.domain.videocall.controller;

import coffeandcommit.crema.domain.videocall.dto.response.QuickJoinResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionConfigResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionStatusResponse;
import coffeandcommit.crema.domain.videocall.service.VideoCallService;
import coffeandcommit.crema.global.common.exception.response.ApiResponse;
import coffeandcommit.crema.global.common.exception.code.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<QuickJoinResponse> quickJoin(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reservationId
            ) {
        QuickJoinResponse response = videoCallService.quickJoin(reservationId, userDetails);
        return ApiResponse.onSuccess(SuccessStatus.OK, response);
    }

    @GetMapping("/config")
    @Operation(summary = "프론트엔드 연결 정보")
    public ApiResponse<SessionConfigResponse> getSessionConfig() {
        SessionConfigResponse configResponse = videoCallService.getFrontendConfig();
        return ApiResponse.onSuccess(SuccessStatus.OK, configResponse);
    }

    @GetMapping("/sessions/{sessionId}/status")
    @Operation(
            summary = "실시간 세션 상태"
    )
    public ApiResponse<SessionStatusResponse> getSessionStatus(@PathVariable String sessionId){
        SessionStatusResponse sessionStatusResponse = videoCallService.getSessionStatus(sessionId);
        return ApiResponse.onSuccess(SuccessStatus.OK, sessionStatusResponse);
    }

    @PostMapping("/sessions/{sessionId}/refresh-token")
    @Operation(
            summary = "토큰 갱신"
    )
    public ApiResponse<QuickJoinResponse> refreshSessionToken(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String sessionId){
        QuickJoinResponse response = videoCallService.refreshToken(sessionId, userDetails.getUsername());
        return ApiResponse.onSuccess(SuccessStatus.OK, response);
    }

    @PostMapping("/sessions/{sessionId}/auto-reconnect")
    @Operation(
            summary = "자동 재연결"
    )
    public ApiResponse<QuickJoinResponse> autoReconnect(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String sessionId,
            @RequestParam(required = false) String lastConnectionId){
        QuickJoinResponse quickJoinResponse = videoCallService.autoReconnect(sessionId, userDetails.getUsername(), lastConnectionId);
        return ApiResponse.onSuccess(SuccessStatus.OK, quickJoinResponse);
    }

}
