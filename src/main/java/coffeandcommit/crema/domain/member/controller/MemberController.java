package coffeandcommit.crema.domain.member.controller;

import coffeandcommit.crema.domain.member.dto.response.MemberResponse;
import coffeandcommit.crema.domain.member.dto.response.MemberPublicResponse;
import coffeandcommit.crema.domain.member.service.MemberProfileService;
import coffeandcommit.crema.domain.member.service.MemberService;
import coffeandcommit.crema.global.common.exception.response.ApiResponse;
import coffeandcommit.crema.global.common.exception.code.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
@Tag(name = "Member API", description = "회원 관리 API")
public class MemberController {

    private final MemberService memberService;
    private final MemberProfileService memberProfileService;

    @Operation(summary = "회원 정보 조회 (ID) - 타인용", description = "회원 ID로 공개 회원 정보를 조회합니다.")
    @SecurityRequirement(name = "JWT")
    @GetMapping("/id/{memberId}")
    public ApiResponse<MemberPublicResponse> getMemberById(
            @Parameter(description = "회원 ID", required = true) @PathVariable String memberId) {
        MemberPublicResponse member = memberService.getMemberById(memberId);
        return ApiResponse.onSuccess(SuccessStatus.OK, member);
    }

    @Operation(summary = "회원 정보 조회 (닉네임) - 타인용", description = "닉네임으로 공개 회원 정보를 조회합니다.")
    @SecurityRequirement(name = "JWT")
    @GetMapping("/nickname/{nickname}")
    public ApiResponse<MemberPublicResponse> getMemberByNickname(
            @Parameter(description = "닉네임", required = true) @PathVariable String nickname) {
        MemberPublicResponse member = memberService.getMemberByNickname(nickname);
        return ApiResponse.onSuccess(SuccessStatus.OK, member);
    }

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 모든 정보를 조회합니다.")
    @SecurityRequirement(name = "JWT")
    @GetMapping("/me")
    public ApiResponse<MemberResponse> getMyInfo(
            @AuthenticationPrincipal UserDetails userDetails) {
        String memberId = userDetails.getUsername(); // JWT에서 member ID 추출
        MemberResponse member = memberService.getMyInfo(memberId);
        return ApiResponse.onSuccess(SuccessStatus.OK, member);
    }

    @Operation(summary = "내 프로필 정보 업데이트", description = "현재 로그인한 사용자의 닉네임과 자기소개를 업데이트합니다.")
    @SecurityRequirement(name = "JWT")
    @PutMapping("/me/profile/info")
    public ApiResponse<MemberResponse> updateMyProfileInfo(
            @Parameter(description = "닉네임") @RequestParam(required = false) String nickname,
            @Parameter(description = "자기소개") @RequestParam(required = false) String description,
            @AuthenticationPrincipal UserDetails userDetails) {

        String memberId = userDetails.getUsername(); // JWT에서 member ID 추출

        MemberResponse updatedMember = memberService.updateMemberProfileInfo(
                memberId, nickname, description);

        log.info("Profile info updated by member: {}", memberId);
        return ApiResponse.onSuccess(SuccessStatus.OK, updatedMember);
    }

    @Operation(summary = "내 프로필 이미지 업데이트", description = "현재 로그인한 사용자의 프로필 이미지를 업로드하고 업데이트합니다.")
    @SecurityRequirement(name = "JWT")
    @PutMapping(value = "/me/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MemberResponse> updateMyProfileImage(
            @Parameter(description = "프로필 이미지 파일", required = true)
            @RequestPart("image") MultipartFile image,
            @AuthenticationPrincipal UserDetails userDetails) {

        String memberId = userDetails.getUsername(); // JWT에서 member ID 추출

        MemberResponse updatedMember = memberProfileService.updateMemberProfileImage(memberId, image);

        log.info("Profile image updated by member: {}", memberId);
        return ApiResponse.onSuccess(SuccessStatus.OK, updatedMember);
    }

    @Operation(summary = "회원 탈퇴", description = "현재 로그인한 사용자가 회원 탈퇴를 진행합니다.")
    @SecurityRequirement(name = "JWT")
    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMember(
            @AuthenticationPrincipal UserDetails userDetails) {

        String memberId = userDetails.getUsername(); // JWT에서 member ID 추출
        memberService.deleteMember(memberId);
        log.info("Member self-deleted: {}", memberId);
        return ApiResponse.onSuccess(SuccessStatus.OK, null);
    }

    @Operation(summary = "닉네임 중복 확인", description = "닉네임 사용 가능 여부를 확인합니다.")
    @GetMapping("/check/nickname/{nickname}")
    public ApiResponse<Boolean> checkNicknameAvailability(
            @Parameter(description = "확인할 닉네임", required = true) @PathVariable String nickname) {
        boolean available = memberService.isNicknameAvailable(nickname);
        return ApiResponse.onSuccess(SuccessStatus.OK, available);
    }
}