package coffeandcommit.crema.domain.videocall.controller;

import coffeandcommit.crema.domain.videocall.dto.response.ParticipantInfoResponse;
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

package coffeandcommit.crema.domain.videocall.controller;

import coffeandcommit.crema.domain.videocall.dto.request.ChatHistorySaveRequest;
import coffeandcommit.crema.domain.videocall.dto.response.ParticipantInfoResponse;
import coffeandcommit.crema.domain.videocall.dto.response.QuickJoinResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionConfigResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionStatusResponse;
import coffeandcommit.crema.domain.videocall.service.VideoCallService;
import coffeandcommit.crema.global.common.exception.response.ApiResponse;
import coffeandcommit.crema.global.common.exception.code.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

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

    @GetMapping("/sessions/{sessionId}/participant")
    @Operation(
            summary = "참가자 정보 조회",
            description = "화상통화 세션의 상대방 참가자 정보를 조회합니다. " +
                         "요청자가 ROOKIE인 경우 GUIDE 정보를, GUIDE인 경우 ROOKIE 정보를 반환합니다."
    )
    public ApiResponse<ParticipantInfoResponse> getParticipantInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String sessionId) {
        // TODO: 구현 필요
        // 1. sessionId로 VideoSession 조회
        // 2. VideoSession의 reservation 정보에서 member, guide 정보 확인
        // 3. 요청자(userDetails.getUsername())의 역할 확인
        // 4. 상대방 정보를 ParticipantInfoResponse로 변환하여 반환
        ParticipantInfoResponse response = videoCallService.getParticipantInfo(sessionId, userDetails.getUsername());
        return ApiResponse.onSuccess(SuccessStatus.OK, response);
    }

    @PostMapping("/sessions/{sessionId}/end")
    @Operation(
            summary = "화상통화 세션 종료",
            description = "세션을 종료하고 채팅 기록을 저장하며 관련 예약 상태를 완료로 변경합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "세션 종료 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "세션 종료 실패")
    })
    public ApiResponse<Void> endSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String sessionId,
            @Valid @RequestBody ChatHistorySaveRequest request) {
        
        videoCallService.endSession(sessionId, request, userDetails.getUsername());
        log.info("세션 종료 완료: sessionId={}, userId={}", sessionId, userDetails.getUsername());
        return ApiResponse.onSuccess(SuccessStatus.OK, null);
    }

}
