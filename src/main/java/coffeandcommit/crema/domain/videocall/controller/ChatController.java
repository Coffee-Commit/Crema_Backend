package coffeandcommit.crema.domain.videocall.controller;

import coffeandcommit.crema.domain.videocall.dto.request.ChatHistorySaveRequest;
import coffeandcommit.crema.domain.videocall.dto.response.ChatHistoryResponse;
import coffeandcommit.crema.domain.videocall.exception.ChatNotFoundException;
import coffeandcommit.crema.domain.videocall.exception.ChatSaveFailedException;
import coffeandcommit.crema.domain.videocall.exception.SessionNotFoundException;
import coffeandcommit.crema.domain.videocall.service.ChatService;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import coffeandcommit.crema.global.common.exception.code.SuccessStatus;
import coffeandcommit.crema.global.common.exception.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/video-call/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "채팅", description = "채팅 기록 관리 API")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/{sessionId}/save")
    @Operation(
            summary = "채팅 기록 저장",
            description = "세션의 채팅 기록을 JSON 형태로 DB에 저장합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "채팅 기록 저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅 기록 업데이트 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "채팅 기록 저장 실패")
    })
    public ResponseEntity<ApiResponse<Void>> saveChatHistory(
            @PathVariable String sessionId,
            @Valid @RequestBody ChatHistorySaveRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            chatService.saveChatHistory(sessionId, request, userDetails.getUsername());
            log.info("채팅 기록 저장 성공: sessionId={}, userId={}", sessionId, userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.onSuccess(SuccessStatus.CREATED, null));
        } catch (SecurityException e) {
            log.error("채팅 저장 권한 없음: sessionId={}, userId={}", sessionId, userDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.onFailure(ErrorStatus.FORBIDDEN, null));
        } catch (ChatSaveFailedException e) {
            log.error("채팅 기록 저장 실패: sessionId={}, userId={}", sessionId, userDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.onFailure(ErrorStatus.CHAT_SAVE_FAILED, null));
        } catch (SessionNotFoundException e) {
            log.error("세션을 찾을 수 없음: sessionId={}, userId={}", sessionId, userDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.onFailure(ErrorStatus.SESSION_NOT_FOUND, null));
        } catch (Exception e) {
            log.error("예상치 못한 오류: sessionId={}, userId={}", sessionId, userDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.onFailure(ErrorStatus.INTERNAL_SERVER_ERROR, null));
        }
    }

    @GetMapping("/{reservationId}/history")
    @Operation(
            summary = "채팅 기록 조회",
            description = "저장된 예약의 채팅 기록을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅 기록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "채팅 기록을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "채팅 기록 조회 실패")
    })
    public ResponseEntity<ApiResponse<ChatHistoryResponse>> getChatHistory(
            @PathVariable String reservationId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            ChatHistoryResponse response = chatService.getChatHistory(reservationId, userDetails.getUsername());
            log.info("채팅 기록 조회 성공: reservationId={}, userId={}", reservationId, userDetails.getUsername());
            return ResponseEntity.ok(ApiResponse.onSuccess(SuccessStatus.OK, response));
        } catch (SecurityException e) {
            log.error("채팅 조회 권한 없음: reservationId={}, userId={}", reservationId, userDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.onFailure(ErrorStatus.FORBIDDEN, null));
        } catch (ChatNotFoundException e) {
            log.error("채팅 기록을 찾을 수 없음: reservationId={}, userId={}", reservationId, userDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.onFailure(ErrorStatus.CHAT_NOT_FOUND, null));
        } catch (Exception e) {
            log.error("채팅 기록 조회 실패: reservationId={}, userId={}", reservationId, userDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.onFailure(ErrorStatus.INTERNAL_SERVER_ERROR, null));
        }
    }
}