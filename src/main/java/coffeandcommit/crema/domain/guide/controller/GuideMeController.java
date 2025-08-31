package coffeandcommit.crema.domain.guide.controller;

import coffeandcommit.crema.domain.guide.dto.request.GuideJobFieldRequestDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideJobFieldResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideProfileResponseDTO;
import coffeandcommit.crema.domain.guide.service.GuideMeService;
import coffeandcommit.crema.global.auth.service.CustomUserDetails;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import coffeandcommit.crema.global.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/guides/me")
@RequiredArgsConstructor
@Tag(name = "GuideMe" , description = "가이드 본인 API")
public class GuideMeController {

    private final GuideMeService guideMeService;

    @Operation(summary = "가이드 본인 프로필 조회", description = "현재 로그인한 가이드의 프로필을 조회합니다.")
    @GetMapping
    public ResponseEntity<Response<GuideProfileResponseDTO>> getGuideMeProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if(userDetails == null){
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        GuideProfileResponseDTO result = guideMeService.getGuideMeProfile(userDetails.getMemberId());

        Response<GuideProfileResponseDTO> response = Response.<GuideProfileResponseDTO>builder()
                .message("조회 완료")
                .data(result)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);


    }

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



}
