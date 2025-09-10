package coffeandcommit.crema.domain.videocall.controller;

import coffeandcommit.crema.domain.videocall.dto.request.SharedFileUploadRequest;
import coffeandcommit.crema.domain.videocall.dto.response.SharedFileListResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SharedFileResponse;
import coffeandcommit.crema.domain.videocall.service.VideoCallFileService;
import coffeandcommit.crema.global.common.exception.response.ApiResponse;
import coffeandcommit.crema.global.common.exception.code.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/video-call")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "화상통화 파일 공유", description = "화상통화 세션 내 파일 공유 관리 API")
public class VideoCallFileController {

    private final VideoCallFileService videoCallFileService;

    @GetMapping("/sessions/{sessionId}/materials")
    @Operation(
        summary = "공유 자료 목록 조회",
        description = "특정 화상통화 세션에 업로드된 공유 자료 목록을 조회합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공유 자료 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "세션 접근 권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    public ApiResponse<SharedFileListResponse> getSharedFiles(
            @Parameter(description = "화상통화 세션 ID", required = true)
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();
        log.info("공유 자료 목록 조회 요청 - 세션: {}, 사용자: {}", sessionId, userId);

        SharedFileListResponse response = videoCallFileService.getSharedFiles(sessionId, userId);

        return ApiResponse.onSuccess(SuccessStatus.OK, response);
    }

    @PostMapping("/sessions/{sessionId}/materials")
    @Operation(
        summary = "공유 자료 등록",
        description = "이미 스토리지에 업로드된 파일을 특정 화상통화 세션과 연결합니다. " +
                     "파일 업로드 자체는 기존 /api/images/upload API를 사용해주세요."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "공유 자료가 성공적으로 등록됨"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 또는 파일이 존재하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "세션 접근 권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 등록된 파일")
    })
    public ApiResponse<SharedFileResponse> addSharedFile(
            @Parameter(description = "화상통화 세션 ID", required = true)
            @PathVariable String sessionId,
            @Parameter(description = "공유 파일 등록 정보", required = true)
            @Valid @RequestBody SharedFileUploadRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();
        log.info("공유 자료 등록 요청 - 세션: {}, 파일: {}, 사용자: {}", sessionId, request.getFileName(), userId);

        SharedFileResponse response = videoCallFileService.addSharedFile(sessionId, request, userId);

        return ApiResponse.onSuccess(SuccessStatus.CREATED, response);
    }

    @DeleteMapping("/sessions/{sessionId}/materials")
    @Operation(
        summary = "공유 자료 삭제",
        description = "세션에서 공유 자료를 삭제합니다. 파일을 업로드한 사용자만 삭제할 수 있습니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공유 자료가 성공적으로 삭제됨"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "삭제 권한 없음 또는 세션 접근 권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션 또는 파일을 찾을 수 없음")
    })
    public ApiResponse<Void> deleteSharedFile(
            @Parameter(description = "화상통화 세션 ID", required = true)
            @PathVariable String sessionId,
            @Parameter(description = "삭제할 파일의 이미지 키", required = true)
            @RequestParam("imageKey") String imageKey,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();
        log.info("공유 자료 삭제 요청 - 세션: {}, 이미지키: {}, 사용자: {}", sessionId, imageKey, userId);

        videoCallFileService.deleteSharedFile(sessionId, imageKey, userId);

        return ApiResponse.onSuccess(SuccessStatus.OK, null);
    }
}