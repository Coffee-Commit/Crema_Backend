package coffeandcommit.crema.domain.guide.controller;

import coffeandcommit.crema.domain.guide.dto.response.GuideJobFieldResponseDTO;
import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.service.GuideService;
import coffeandcommit.crema.global.auth.service.CustomUserDetails;
import coffeandcommit.crema.global.common.response.Response;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/guides")
@RequiredArgsConstructor
@Tag(name = "Guide" , description = "가이드 API")
public class GuideController {

    private final GuideService guideService;

    @GetMapping("/{guideId}/job-field")
    public ResponseEntity<Response<GuideJobFieldResponseDTO>> getGuideJobField(
            @PathVariable Long guideId,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {

        String loginMemberId = userDetails.getMemberId();

        GuideJobFieldResponseDTO result = guideService.getGuideJobField(guideId, loginMemberId);

        Response<GuideJobFieldResponseDTO> response = Response.<GuideJobFieldResponseDTO>builder()
                .message("가이드 직무분야 조회 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }
}
