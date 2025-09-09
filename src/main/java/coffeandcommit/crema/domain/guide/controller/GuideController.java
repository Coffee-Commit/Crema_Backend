package coffeandcommit.crema.domain.guide.controller;

import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
import coffeandcommit.crema.domain.guide.dto.response.*;
import coffeandcommit.crema.domain.guide.service.GuideService;
import coffeandcommit.crema.global.auth.service.CustomUserDetails;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import coffeandcommit.crema.global.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/guides")
@RequiredArgsConstructor
@Tag(name = "Guide" , description = "가이드 API")
public class GuideController {

    private final GuideService guideService;

    @Operation(summary = "가이드 직무분야 조회", description = "특정 가이드의 직무분야를 조회합니다. 비공개 가이드인 경우 본인 가이드만 조회할 수 있습니다.")
    @GetMapping("/{guideId}/job-field")
    public ResponseEntity<Response<GuideJobFieldResponseDTO>> getGuideJobField(
            @PathVariable Long guideId,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {

        String loginMemberId = (userDetails != null) ? userDetails.getMemberId() : null;

        GuideJobFieldResponseDTO result = guideService.getGuideJobField(guideId, loginMemberId);

        Response<GuideJobFieldResponseDTO> response = Response.<GuideJobFieldResponseDTO>builder()
                .message("가이드 직무분야 조회 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "가이드 챗 주제 조회", description = "특정 가이드의 챗 주제를 조회합니다. 비공개 가이드인 경우 본인 가이드만 조회할 수 있습니다.")
    @GetMapping("/{guideId}/chat-topics")
    public ResponseEntity<Response<List<GuideChatTopicResponseDTO>>> getGuideChatTopics(
            @PathVariable Long guideId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String loginMemberId = (userDetails != null) ? userDetails.getMemberId() : null;

        List<GuideChatTopicResponseDTO> result = guideService.getGuideChatTopics(guideId, loginMemberId);

        Response<List<GuideChatTopicResponseDTO>> response = Response.<List<GuideChatTopicResponseDTO>>builder()
                .message("가이드 챗 주제 조회 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "가이드 해시태그 조회", description = "특정 가이드의 해시태그를 조회합니다. 비공개 가이드인 경우 본인 가이드만 조회할 수 있습니다.")
    @GetMapping("/{guideId}/hashtags")
    public ResponseEntity<Response<List<GuideHashTagResponseDTO>>> getGuideHashTags(
            @PathVariable Long guideId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String loginMemberId = (userDetails != null) ? userDetails.getMemberId() : null;

        List<GuideHashTagResponseDTO> result = guideService.getGuideHashTags(guideId, loginMemberId);

        Response<List<GuideHashTagResponseDTO>> response = Response.<List<GuideHashTagResponseDTO>>builder()
                .message("가이드 해시태그 조회 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "가이드 스케줄 조회", description = "특정 가이드의 스케줄을 조회합니다. 비공개 가이드인 경우 본인 가이드만 조회할 수 있습니다.")
    @GetMapping("/{guideId}/schedules")
    public ResponseEntity<Response<GuideScheduleResponseDTO>> getGuideSchedules(
            @PathVariable Long guideId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String loginMemberId = (userDetails != null) ? userDetails.getMemberId() : null;

        GuideScheduleResponseDTO result = guideService.getGuideSchedules(guideId, loginMemberId);

        Response<GuideScheduleResponseDTO> response = Response.<GuideScheduleResponseDTO>builder()
                .message("가이드 스케줄 조회 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "가이드 경험 소주제 조회", description = "특정 가이드의 경험 소주제를 조회합니다. 비공개 가이드인 경우 본인 가이드만 조회할 수 있습니다.")
    @GetMapping("/{guideId}/experiences/details")
    public ResponseEntity<Response<GuideExperienceDetailResponseDTO>> getGuideExperiencesDetails(
            @PathVariable Long guideId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String loginMemberId = (userDetails != null) ? userDetails.getMemberId() : null;

        GuideExperienceDetailResponseDTO result = guideService.getGuideExperienceDetails(guideId, loginMemberId);

        Response<GuideExperienceDetailResponseDTO> response = Response.<GuideExperienceDetailResponseDTO>builder()
                .message("가이드 경험 소주제 조회 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "가이드 경험 목록 조회", description = "특정 가이드의 경험 목록을 조회합니다. 비공개 가이드인 경우 본인 가이드만 조회할 수 있습니다.")
    @GetMapping("/{guideId}/experiences")
    public ResponseEntity<Response<GuideExperienceResponseDTO>> getGuideExperiences(
            @PathVariable Long guideId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String loginMemberId = (userDetails != null) ? userDetails.getMemberId() : null;

        GuideExperienceResponseDTO result = guideService.getGuideExperiences(guideId, loginMemberId);

        Response<GuideExperienceResponseDTO> response = Response.<GuideExperienceResponseDTO>builder()
                .message("가이드 경험 목록 조회 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "가이드 커피챗 조회", description = "특정 가이드의 커피챗 정보를 조회합니다.")
    @GetMapping("/{guideId}/coffeechats")
    public ResponseEntity<Response<GuideCoffeeChatResponseDTO>> getGuideCoffeeChat(
            @PathVariable Long guideId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String loginMemberId = (userDetails != null) ? userDetails.getMemberId() : null;

        GuideCoffeeChatResponseDTO result = guideService.getGuideCoffeeChat(guideId, loginMemberId);

        Response<GuideCoffeeChatResponseDTO> response = Response.<GuideCoffeeChatResponseDTO>builder()
                .message("가이드 커피챗 조회 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);

    }

    @Operation(summary = "이번주 커피챗 조회", description = "특정 가이드의 이번주 커피챗을 조회합니다.")
    @GetMapping("/{guideId}/coffeechats/this-week")
    public ResponseEntity<Response<Page<GuideThisWeekCoffeeChatResponseDTO>>> getThisWeekCoffeeChats(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long guideId,
            Pageable pageable) {

        if (userDetails == null) {
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        Page<GuideThisWeekCoffeeChatResponseDTO> result = guideService.getThisWeekCoffeeChats(guideId, loginMemberId, pageable);

        Response<Page<GuideThisWeekCoffeeChatResponseDTO>> response = Response.<Page<GuideThisWeekCoffeeChatResponseDTO>>builder()
                .message("이번주 커피챗 조회 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "가이드 커피챗 통계 조회", description = "특정 가이드의 커피챗 통계를 조회합니다.")
    @GetMapping("/{guideId}/coffeechat-stats")
    public ResponseEntity<Response<CoffeeChatStatsResponseDTO>> getCoffeeChatStats(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long guideId) {

        if (userDetails == null) {
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        CoffeeChatStatsResponseDTO result = guideService.getCoffeeChatStats(guideId, loginMemberId);

        Response<CoffeeChatStatsResponseDTO> response = Response.<CoffeeChatStatsResponseDTO>builder()
                .message("가이드 커피챗 통계 조회 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "가이드 경험 평가 조회", description = "특정 가이드의 경험 평가를 조회합니다.")
    @GetMapping("/{guideId}/experience-evaluations")
    public ResponseEntity<Response<List<GuideExperienceEvaluationResponseDTO>>> getGuideExperienceEvaluations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long guideId) {

        if (userDetails == null) {
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        List<GuideExperienceEvaluationResponseDTO> result =
                guideService.getGuideExperienceEvaluations(guideId, loginMemberId);

        Response<List<GuideExperienceEvaluationResponseDTO>> response =
                Response.<List<GuideExperienceEvaluationResponseDTO>>builder()
                        .message("가이드 경험 평가 조회 성공")
                        .data(result)
                        .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "가이드 리뷰 조회", description = "특정 가이드의 리뷰를 조회합니다.")
    @GetMapping("/{guideId}/reviews")
    public ResponseEntity<Response<Page<GuideReviewResponseDTO>>> getGuideReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long guideId,
            Pageable pageable) {

        if (userDetails == null) {
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        Page<GuideReviewResponseDTO> result = guideService.getGuideReviews(guideId, loginMemberId, pageable);

        // 4. 응답 Wrapping
        Response<Page<GuideReviewResponseDTO>> response = Response.<Page<GuideReviewResponseDTO>>builder()
                .message("가이드 리뷰 조회 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "가이드 프로필 조회", description = "특정 가이드의 프로필을 조회합니다.")
    @GetMapping("/{guideId}/profile")
    public ResponseEntity<Response<GuideProfileResponseDTO>> getGuideProfile(
            @PathVariable Long guideId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        GuideProfileResponseDTO result = guideService.getGuideProfile(guideId, loginMemberId);

        Response<GuideProfileResponseDTO> response = Response.<GuideProfileResponseDTO>builder()
                .message("가이드 프로필 조회 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Response<Page<GuideListResponseDTO>>> getGuides(
            @RequestParam(required = false) List<JobNameType> jobNames,
            @RequestParam(required = false) List<TopicNameType> chatTopicNames,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "latest") @Pattern(regexp = "latest|popular") String sort,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        if (userDetails == null) {
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        // Pageable은 sort 기준 상관없이 페이지네이션 정보만 넘김
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "modifiedAt"));

        Page<GuideListResponseDTO> guides =
                guideService.getGuides(jobNames, chatTopicNames, keyword, pageable, loginMemberId, sort);

        Response<Page<GuideListResponseDTO>> response = Response.<Page<GuideListResponseDTO>>builder()
                .message("가이드 목록 조회 성공")
                .data(guides)
                .build();

        return ResponseEntity.ok(response);
    }

}
