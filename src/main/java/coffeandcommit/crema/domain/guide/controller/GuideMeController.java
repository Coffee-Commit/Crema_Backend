package coffeandcommit.crema.domain.guide.controller;

import coffeandcommit.crema.domain.guide.dto.request.*;
import coffeandcommit.crema.domain.guide.dto.response.*;
import coffeandcommit.crema.domain.guide.service.GuideMeService;
import coffeandcommit.crema.global.auth.service.CustomUserDetails;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import coffeandcommit.crema.global.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/guides/me")
@RequiredArgsConstructor
@Tag(name = "GuideMe" , description = "가이드 본인 API")
public class GuideMeController {

    private final GuideMeService guideMeService;

    @Operation(summary = "가이드 직무 분야 등록", description = "가이드의 직무 분야를 등록합니다.")
    @PostMapping("/job-field")
    public ResponseEntity<Response<GuideJobFieldResponseDTO>> registerJobField(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody GuideJobFieldRequestDTO guideJobFieldRequestDTO) {

        if(userDetails == null){
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        GuideJobFieldResponseDTO result = guideMeService.registerGuideJobField(userDetails.getMemberId(), guideJobFieldRequestDTO);

        Response<GuideJobFieldResponseDTO> response = Response.<GuideJobFieldResponseDTO>builder()
                .message("직무 분야 등록 완료")
                .data(result)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }


    @Operation(summary = "가이드 채팅 주제 등록", description = "가이드의 채팅 주제를 등록합니다.")
    @PostMapping("/chat-topics")
    public ResponseEntity<Response<List<GuideChatTopicResponseDTO>>> registerChatTopics(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody GuideChatTopicRequestDTO guideChatTopicRequestDTO){

        if(userDetails == null){
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        if (guideChatTopicRequestDTO == null
                || guideChatTopicRequestDTO.getTopics() == null
                || guideChatTopicRequestDTO.getTopics().isEmpty()) {
            throw new BaseException(ErrorStatus.INVALID_TOPIC);
        }

        String loginMemberId = userDetails.getMemberId();

        List<GuideChatTopicResponseDTO> result = guideMeService.registerChatTopics(loginMemberId, guideChatTopicRequestDTO);

        Response<List<GuideChatTopicResponseDTO>> response = Response.<List<GuideChatTopicResponseDTO>>builder()
                .message("채팅 주제 등록 완료")
                .data(result)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "가이드 채팅 주제 삭제", description = "가이드의 채팅 주제를 삭제합니다.")
    @DeleteMapping("/chat-topics/{topicId}")
    public ResponseEntity<Response<List<GuideChatTopicResponseDTO>>> deleteChatTopic(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("topicId") Long topicId) {

        if(userDetails == null){
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }
        String loginMemberId = userDetails.getMemberId();

        List<GuideChatTopicResponseDTO> result = guideMeService.deleteChatTopic(loginMemberId, topicId);

        Response<List<GuideChatTopicResponseDTO>> response = Response.<List<GuideChatTopicResponseDTO>>builder()
                .message("채팅 주제 삭제 완료")
                .data(result)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "가이드 해시태그 등록", description = "가이드의 해시태그를 등록합니다. 최대 5개까지 등록할 수 있습니다.")
    @PostMapping("/hashtags")
    public ResponseEntity<Response<List<GuideHashTagResponseDTO>>> registerGuideHashTags(
            @Valid @NotEmpty
            @RequestBody List<GuideHashTagRequestDTO> guideHashTagRequestDTOs,
            @AuthenticationPrincipal CustomUserDetails userDetails){

        if(userDetails == null){
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        List<GuideHashTagResponseDTO> result = guideMeService.registerGuideHashTags(loginMemberId, guideHashTagRequestDTOs);

        Response<List<GuideHashTagResponseDTO>> response = Response.<List<GuideHashTagResponseDTO>>builder()
                .message("가이드 해시태그 등록 완료")
                .data(result)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

    @Operation(summary = "가이드 해시태그 삭제", description = "가이드의 해시태그를 삭제합니다.")
    @DeleteMapping("/hashtags/{hashTagId}")
    public ResponseEntity<Response<List<GuideHashTagResponseDTO>>> deleteGuideHashTag(
            @PathVariable Long hashTagId,
            @AuthenticationPrincipal CustomUserDetails userDetails){

        if(userDetails == null){
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        List<GuideHashTagResponseDTO> result = guideMeService.deleteGuideHashTag(loginMemberId, hashTagId);

        Response<List<GuideHashTagResponseDTO>> response = Response.<List<GuideHashTagResponseDTO>>builder()
                .message("가이드 해시태그 삭제 완료")
                .data(result)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @Operation(summary = "가이드 스케줄 등록", description = "가이드의 스케줄을 등록합니다.")
    @PostMapping("/schedules")
    public ResponseEntity<Response<GuideScheduleResponseDTO>> registerGuideSchedules(
            @Valid @RequestBody GuideScheduleRequestDTO guideScheduleRequestDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails){

        if(userDetails == null){
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        GuideScheduleResponseDTO result = guideMeService.registerGuideSchedules(loginMemberId, guideScheduleRequestDTO);

        Response<GuideScheduleResponseDTO> response = Response.<GuideScheduleResponseDTO>builder()
                .message("가이드 스케줄 등록 완료")
                .data(result)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

    @Operation(summary = "가이드 스케줄 삭제", description = "가이드의 스케줄을 삭제합니다.")
    @DeleteMapping("/schedules/{timeSlotId}")
    public ResponseEntity<Response<GuideScheduleResponseDTO>> deleteGuideSchedule(
            @PathVariable Long timeSlotId,
            @AuthenticationPrincipal CustomUserDetails userDetails){

        if(userDetails == null){
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        GuideScheduleResponseDTO result = guideMeService.deleteGuideSchedule(loginMemberId, timeSlotId);

        Response<GuideScheduleResponseDTO> response = Response.<GuideScheduleResponseDTO>builder()
                .message("가이드 스케줄 삭제 완료")
                .data(result)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "가이드 경험 소주제 등록", description = "가이드의 경험 소주제를 등록합니다.")
    @PostMapping("/experiences/details")
    public ResponseEntity<Response<GuideExperienceDetailResponseDTO>> registerExperienceDetail(
            @Valid @RequestBody GuideExperienceDetailRequestDTO guideExperienceDetailRequestDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails){

        if(userDetails == null){
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        GuideExperienceDetailResponseDTO result = guideMeService.registerExperienceDetail(loginMemberId, guideExperienceDetailRequestDTO);

        Response<GuideExperienceDetailResponseDTO> response = Response.<GuideExperienceDetailResponseDTO>builder()
                .message("가이드 경험 소주제 등록 완료")
                .data(result)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

    @Operation(summary = "가이드 경험 소주제 삭제", description = "가이드의 경험 소주제를 삭제합니다.")
    @DeleteMapping("/experiences/details/{experienceDetailId}")
    public ResponseEntity<Response<GuideExperienceDetailResponseDTO>> deleteExperienceDetail(
            @PathVariable Long experienceDetailId,
            @AuthenticationPrincipal CustomUserDetails userDetails){

        if(userDetails == null){
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        GuideExperienceDetailResponseDTO result = guideMeService.deleteExperienceDetail(loginMemberId, experienceDetailId);

        Response<GuideExperienceDetailResponseDTO> response = Response.<GuideExperienceDetailResponseDTO>builder()
                .message("가이드 경험 소주제 삭제 완료")
                .data(result)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @Operation(summary = "가이드 경험 목록 등록", description = "가이드의 경험 목록을 등록합니다.")
    @PostMapping("/experiences")
    public ResponseEntity<Response<GuideExperienceResponseDTO>> registerGuideExperience(
            @Valid @RequestBody GuideExperienceRequestDTO guideExperienceRequestDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails){

        if(userDetails == null){
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        GuideExperienceResponseDTO result = guideMeService.registerGuideExperience(loginMemberId, guideExperienceRequestDTO);

        Response<GuideExperienceResponseDTO> response = Response.<GuideExperienceResponseDTO>builder()
                .message("가이드 경험 목록 등록 완료")
                .data(result)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

    @Operation(summary = "가이드 경험 목록 삭제", description = "가이드의 경험 목록을 삭제합니다.")
    @DeleteMapping("/experiences/{experienceId}")
    public ResponseEntity<Response<GuideExperienceResponseDTO>> deleteGuideExperience(
            @PathVariable Long experienceId,
            @AuthenticationPrincipal CustomUserDetails userDetails){

        if(userDetails == null){
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        GuideExperienceResponseDTO result = guideMeService.deleteGuideExperience(loginMemberId, experienceId);

        Response<GuideExperienceResponseDTO> response = Response.<GuideExperienceResponseDTO>builder()
                .message("가이드 경험 목록 삭제 완료")
                .data(result)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @Operation(summary = "가이드 커피챗 등록", description = "가이드의 커피챗을 등록합니다.")
    @PostMapping("/coffeechat")
    public ResponseEntity<Response<GuideCoffeeChatResponseDTO>> registerGuideCoffeeChat(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody GuideCoffeeChatRequestDTO requestDTO) {

        if (userDetails == null) {
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        GuideCoffeeChatResponseDTO result = guideMeService.registerGuideCoffeeChat(loginMemberId, requestDTO);

        Response<GuideCoffeeChatResponseDTO> response = Response.<GuideCoffeeChatResponseDTO>builder()
                .message("가이드 커피챗 등록 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "가이드 대기중 커피챗 조회", description = "가이드 본인의 가이드 대기중 커피챗 목록을 조회합니다.")
    @GetMapping("/reservations/pending")
    public ResponseEntity<Response<List<GuidePendingReservationResponseDTO>>> getPendingReservations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        List<GuidePendingReservationResponseDTO> result = guideMeService.getPendingReservations(loginMemberId);

        Response<List<GuidePendingReservationResponseDTO>> response = Response.<List<GuidePendingReservationResponseDTO>>builder()
                .message("가이드 대기중 커피챗 조회 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "가이드 공개 상태 변경", description = "가이드의 공개 상태를 변경합니다.")
    @PatchMapping("/visibility")
    public ResponseEntity<Response<Void>> updateGuideVisibility(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody GuideVisibilityRequestDTO guideVisibilityRequestDTO) {

        if (userDetails == null) {
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }
        String loginMemberId = userDetails.getMemberId();

        guideMeService.updateGuideVisibility(loginMemberId, guideVisibilityRequestDTO);

        Response<Void> response = Response.<Void>builder()
                .message("가이드 공개 상태 변경 완료")
                .data(null)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "내 리뷰 조회", description = "가이드 본인의 리뷰 목록을 조회합니다.")
    @GetMapping("/reviews")
    public ResponseEntity<Response<Page<GuideReviewResponseDTO>>> getMyReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable
    ) {
        if (userDetails == null) {
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();
        Pageable capped = PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.min(pageable.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<GuideReviewResponseDTO> result = guideMeService.getMyGuideReviews(loginMemberId, capped);

        Response<Page<GuideReviewResponseDTO>> response = Response.<Page<GuideReviewResponseDTO>>builder()
                .message("가이드본인리뷰조회성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "가이드 전체 커피챗 조회", description = "가이드의 모든 상태 커피챗 예약을 조회합니다.")
    @GetMapping("/reservations/all")
    public ResponseEntity<Response<Page<GuidePendingReservationResponseDTO>>> getAllReservations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        if (userDetails == null) {
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.fromString(direction), sortBy)
        );

        Page<GuidePendingReservationResponseDTO> result = guideMeService.getAllReservations(loginMemberId, pageable);

        Response<Page<GuidePendingReservationResponseDTO>> response = Response.<Page<GuidePendingReservationResponseDTO>>builder()
                .message("가이드 전체 커피챗 조회 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

}
