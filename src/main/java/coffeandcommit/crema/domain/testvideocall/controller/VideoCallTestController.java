package coffeandcommit.crema.domain.testvideocall.controller;

import coffeandcommit.crema.domain.testvideocall.service.VideoCallTestService;
import coffeandcommit.crema.domain.videocall.dto.response.QuickJoinResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionConfigResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SessionStatusResponse;
import coffeandcommit.crema.global.common.exception.response.ApiResponse;
import coffeandcommit.crema.global.common.exception.code.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test/video-call")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "화상통화 테스트", description = "OpenVidu 화상통화 테스트 API (인증 불필요)")
public class VideoCallTestController {

    private final VideoCallTestService videoCallTestService;

    @PostMapping("/quick-join")
    @Operation(
            summary = "세션 참가(자동 세션생성) - 테스트용",
            description = "인증 없이 세션에 참가할 수 있는 테스트용 API입니다."
    )
    public ApiResponse<QuickJoinResponse> quickJoin(
            @RequestParam String username,
            @RequestParam String sessionName
            ) {
        long startTime = System.currentTimeMillis();
        log.info("[QUICKJOIN-REQUEST] 테스트 quickjoin 요청 시작 - username: {}, sessionName: {}", username, sessionName);
        
        // 입력 파라미터 검증 로깅
        if (username == null || username.trim().isEmpty()) {
            log.error("[QUICKJOIN-REQUEST] 잘못된 username 파라미터: {}", username);
        }
        if (sessionName == null || sessionName.trim().isEmpty()) {
            log.error("[QUICKJOIN-REQUEST] 잘못된 sessionName 파라미터: {}", sessionName);
        }
        
        try {
            QuickJoinResponse response = videoCallTestService.quickJoin(sessionName, username);
            long processingTime = System.currentTimeMillis() - startTime;
            
            log.info("[QUICKJOIN-RESPONSE] 테스트 quickjoin 요청 성공 - username: {}, sessionName: {}, " +
                    "sessionId: {}, 처리시간: {}ms", username, sessionName, response.getSessionId(), processingTime);
            
            return ApiResponse.onSuccess(SuccessStatus.OK, response);
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("[QUICKJOIN-ERROR] 테스트 quickjoin 요청 실패 - username: {}, sessionName: {}, " +
                    "처리시간: {}ms, error: {}", username, sessionName, processingTime, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/config")
    @Operation(summary = "프론트엔드 연결 정보 - 테스트용")
    public ApiResponse<SessionConfigResponse> getSessionConfig() {
        SessionConfigResponse configResponse = videoCallTestService.getFrontendConfig();
        return ApiResponse.onSuccess(SuccessStatus.OK, configResponse);
    }

    @GetMapping("/sessions/{sessionId}/status")
    @Operation(
            summary = "실시간 세션 상태 - 테스트용"
    )
    public ApiResponse<SessionStatusResponse> getSessionStatus(@PathVariable String sessionId){
        SessionStatusResponse sessionStatusResponse = videoCallTestService.getSessionStatus(sessionId);
        return ApiResponse.onSuccess(SuccessStatus.OK, sessionStatusResponse);
    }

    @PostMapping("/sessions/{sessionId}/refresh-token")
    @Operation(
            summary = "토큰 갱신 - 테스트용"
    )
    public ApiResponse<QuickJoinResponse> refreshSessionToken(
            @RequestParam String username,
            @PathVariable String sessionId){
        QuickJoinResponse response = videoCallTestService.refreshToken(sessionId, username);
        return ApiResponse.onSuccess(SuccessStatus.OK, response);
    }

    @PostMapping("/sessions/{sessionId}/auto-reconnect")
    @Operation(
            summary = "자동 재연결 - 테스트용"
    )
    public ApiResponse<QuickJoinResponse> autoReconnect(
            @RequestParam String username,
            @PathVariable String sessionId,
            @RequestParam(required = false) String lastConnectionId){
        QuickJoinResponse quickJoinResponse = videoCallTestService.autoReconnect(sessionId, username, lastConnectionId);
        return ApiResponse.onSuccess(SuccessStatus.OK, quickJoinResponse);
    }

}