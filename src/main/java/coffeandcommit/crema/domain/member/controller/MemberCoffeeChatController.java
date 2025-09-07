package coffeandcommit.crema.domain.member.controller;

import coffeandcommit.crema.domain.member.dto.request.MemberChatTopicRequest;
import coffeandcommit.crema.domain.member.dto.request.MemberJobFieldRequest;
import coffeandcommit.crema.domain.member.dto.response.MemberChatTopicResponse;
import coffeandcommit.crema.domain.member.dto.response.MemberJobFieldResponse;
import coffeandcommit.crema.domain.member.dto.response.MemberCoffeeChatResponse;
import coffeandcommit.crema.domain.member.service.MemberCoffeeChatService;
import coffeandcommit.crema.global.common.exception.response.ApiResponse;
import coffeandcommit.crema.global.common.exception.code.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/member/coffee-chat")
@RequiredArgsConstructor
@Tag(name = "Member Coffee Chat API", description = "멤버 커피챗 관련 API")
public class MemberCoffeeChatController {

    private final MemberCoffeeChatService memberCoffeeChatService;

    // === 관심 주제 관리 ===

    @Operation(summary = "관심 커피챗 주제 등록", description = "멤버의 관심 커피챗 주제를 등록합니다. 여러개 선택 가능하며, 아무것도 선택하지 않으면 UNDEFINED로 설정됩니다.")
    @SecurityRequirement(name = "JWT")
    @PostMapping("/interests/topics")
    public ApiResponse<List<MemberChatTopicResponse>> registerChatTopics(
            @Valid @RequestBody MemberChatTopicRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String memberId = userDetails.getUsername();
        List<MemberChatTopicResponse> result = memberCoffeeChatService.registerChatTopics(memberId, request);

        log.info("Chat topics registered by member: {}", memberId);
        return ApiResponse.onSuccess(SuccessStatus.CREATED, result);
    }

    @Operation(summary = "관심 커피챗 주제 수정", description = "멤버의 관심 커피챗 주제를 수정합니다. 여러개 선택 가능하며, 아무것도 선택하지 않으면 UNDEFINED로 설정됩니다.")
    @SecurityRequirement(name = "JWT")
    @PutMapping("/interests/topics")
    public ApiResponse<List<MemberChatTopicResponse>> updateChatTopics(
            @Valid @RequestBody MemberChatTopicRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String memberId = userDetails.getUsername();
        List<MemberChatTopicResponse> result = memberCoffeeChatService.updateChatTopics(memberId, request);

        log.info("Chat topics updated by member: {}", memberId);
        return ApiResponse.onSuccess(SuccessStatus.OK, result);
    }

    @Operation(summary = "관심 커피챗 주제 조회", description = "멤버의 관심 커피챗 주제를 조회합니다.")
    @SecurityRequirement(name = "JWT")
    @GetMapping("/interests/topics")
    public ApiResponse<List<MemberChatTopicResponse>> getChatTopics(
            @AuthenticationPrincipal UserDetails userDetails) {

        String memberId = userDetails.getUsername();
        List<MemberChatTopicResponse> result = memberCoffeeChatService.getChatTopics(memberId);

        return ApiResponse.onSuccess(SuccessStatus.OK, result);
    }

    // === 관심 분야 관리 ===

    @Operation(summary = "관심 커피챗 분야 등록", description = "멤버의 관심 커피챗 분야를 등록합니다. 하나만 선택 가능하며, 미선택시 UNDEFINED로 설정됩니다.")
    @SecurityRequirement(name = "JWT")
    @PostMapping("/interests/fields")
    public ApiResponse<MemberJobFieldResponse> registerJobField(
            @Valid @RequestBody MemberJobFieldRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String memberId = userDetails.getUsername();
        MemberJobFieldResponse result = memberCoffeeChatService.registerJobField(memberId, request);

        log.info("Job field registered by member: {}", memberId);
        return ApiResponse.onSuccess(SuccessStatus.CREATED, result);
    }

    @Operation(summary = "관심 커피챗 분야 수정", description = "멤버의 관심 커피챗 분야를 수정합니다. 하나만 선택 가능하며, 미선택시 UNDEFINED로 설정됩니다.")
    @SecurityRequirement(name = "JWT")
    @PutMapping("/interests/fields")
    public ApiResponse<MemberJobFieldResponse> updateJobField(
            @Valid @RequestBody MemberJobFieldRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String memberId = userDetails.getUsername();
        MemberJobFieldResponse result = memberCoffeeChatService.updateJobField(memberId, request);

        log.info("Job field updated by member: {}", memberId);
        return ApiResponse.onSuccess(SuccessStatus.OK, result);
    }

    @Operation(summary = "관심 커피챗 분야 조회", description = "멤버의 관심 커피챗 분야를 조회합니다.")
    @SecurityRequirement(name = "JWT")
    @GetMapping("/interests/fields")
    public ApiResponse<MemberJobFieldResponse> getJobField(
            @AuthenticationPrincipal UserDetails userDetails) {

        String memberId = userDetails.getUsername();
        MemberJobFieldResponse result = memberCoffeeChatService.getJobField(memberId);

        return ApiResponse.onSuccess(SuccessStatus.OK, result);
    }

    // === 커피챗 예약 조회 ===

    @Operation(summary = "대기중 커피챗 조회", description = "멤버의 대기중(PENDING) 상태 커피챗을 조회합니다.")
    @SecurityRequirement(name = "JWT")
    @GetMapping("/reservations/pending")
    public ApiResponse<List<MemberCoffeeChatResponse>> getPendingReservations(
            @AuthenticationPrincipal UserDetails userDetails) {

        String memberId = userDetails.getUsername();
        List<MemberCoffeeChatResponse> result = memberCoffeeChatService.getPendingReservations(memberId);

        return ApiResponse.onSuccess(SuccessStatus.OK, result);
    }

    @Operation(summary = "확정된 커피챗 조회", description = "멤버의 확정된(CONFIRMED) 상태 커피챗을 조회합니다.")
    @SecurityRequirement(name = "JWT")
    @GetMapping("/reservations/confirmed")
    public ApiResponse<List<MemberCoffeeChatResponse>> getConfirmedReservations(
            @AuthenticationPrincipal UserDetails userDetails) {

        String memberId = userDetails.getUsername();
        List<MemberCoffeeChatResponse> result = memberCoffeeChatService.getConfirmedReservations(memberId);

        return ApiResponse.onSuccess(SuccessStatus.OK, result);
    }

    @Operation(summary = "완료된 커피챗 조회", description = "멤버의 완료된(COMPLETED) 상태 커피챗을 조회합니다.")
    @SecurityRequirement(name = "JWT")
    @GetMapping("/reservations/completed")
    public ApiResponse<List<MemberCoffeeChatResponse>> getCompletedReservations(
            @AuthenticationPrincipal UserDetails userDetails) {

        String memberId = userDetails.getUsername();
        List<MemberCoffeeChatResponse> result = memberCoffeeChatService.getCompletedReservations(memberId);

        return ApiResponse.onSuccess(SuccessStatus.OK, result);
    }

    @Operation(summary = "취소된 커피챗 조회", description = "멤버의 취소된(CANCELLED) 상태 커피챗을 조회합니다.")
    @SecurityRequirement(name = "JWT")
    @GetMapping("/reservations/cancelled")
    public ApiResponse<List<MemberCoffeeChatResponse>> getCancelledReservations(
            @AuthenticationPrincipal UserDetails userDetails) {

        String memberId = userDetails.getUsername();
        List<MemberCoffeeChatResponse> result = memberCoffeeChatService.getCancelledReservations(memberId);

        return ApiResponse.onSuccess(SuccessStatus.OK, result);
    }

    @Operation(summary = "전체 커피챗 조회", description = "멤버의 모든 상태 커피챗을 조회합니다.")
    @SecurityRequirement(name = "JWT")
    @GetMapping("/reservations/all")
    public ApiResponse<List<MemberCoffeeChatResponse>> getAllReservations(
            @AuthenticationPrincipal UserDetails userDetails) {

        String memberId = userDetails.getUsername();
        List<MemberCoffeeChatResponse> result = memberCoffeeChatService.getAllReservations(memberId);

        return ApiResponse.onSuccess(SuccessStatus.OK, result);
    }
}