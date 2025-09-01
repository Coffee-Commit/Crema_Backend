package coffeandcommit.crema.domain.guide.controller;

import coffeandcommit.crema.domain.guide.dto.response.GuideChatTopicResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideJobFieldResponseDTO;
import coffeandcommit.crema.domain.guide.service.GuideService;
import coffeandcommit.crema.global.auth.service.CustomUserDetails;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import coffeandcommit.crema.global.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

        if(userDetails == null){
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

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

        if(userDetails == null){
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        List<GuideChatTopicResponseDTO> result = guideService.getGuideChatTopics(guideId, loginMemberId);

        Response<List<GuideChatTopicResponseDTO>> response = Response.<List<GuideChatTopicResponseDTO>>builder()
                .message("가이드 챗 주제 조회 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

}
