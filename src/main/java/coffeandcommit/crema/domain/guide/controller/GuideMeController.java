package coffeandcommit.crema.domain.guide.controller;

import coffeandcommit.crema.domain.guide.dto.request.GuideChatTopicRequestDTO;
import coffeandcommit.crema.domain.guide.dto.request.GuideHashTagRequestDTO;
import coffeandcommit.crema.domain.guide.dto.request.GuideJobFieldRequestDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideChatTopicResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideHashTagResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideJobFieldResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideProfileResponseDTO;
import coffeandcommit.crema.domain.guide.service.GuideMeService;
import coffeandcommit.crema.global.auth.service.CustomUserDetails;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import coffeandcommit.crema.global.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Literal;
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


}
