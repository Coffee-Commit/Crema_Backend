package coffeandcommit.crema.domain.member.controller;

import coffeandcommit.crema.domain.member.dto.response.MemberResponse;
import coffeandcommit.crema.domain.member.service.MemberService;
import coffeandcommit.crema.global.common.exception.response.ApiResponse;
import coffeandcommit.crema.global.common.exception.code.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
@Tag(name = "Member API", description = "회원 관리 API")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "회원 정보 조회 (사용자 아이디)", description = "사용자 아이디(이메일)로 회원 정보를 조회합니다.")
    @SecurityRequirement(name = "JWT")
    @GetMapping("/userId/{userId}")
    public ApiResponse<MemberResponse> getMemberByUserId(
            @Parameter(description = "사용자 아이디 (이메일)", required = true) @PathVariable String userId) {
        MemberResponse member = memberService.getMemberByUserId(userId);
        return ApiResponse.onSuccess(SuccessStatus.OK, member);
    }

    @Operation(summary = "회원 정보 조회 (닉네임)", description = "닉네임으로 회원 정보를 조회합니다.")
    @SecurityRequirement(name = "JWT")
    @GetMapping("/nickname/{nickname}")
    public ApiResponse<MemberResponse> getMemberByNickname(
            @Parameter(description = "닉네임", required = true) @PathVariable String nickname) {
        MemberResponse member = memberService.getMemberByNickname(nickname);
        return ApiResponse.onSuccess(SuccessStatus.OK, member);
    }

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @SecurityRequirement(name = "JWT")
    @GetMapping("/me")
    public ApiResponse<MemberResponse> getMyInfo(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername(); // Spring Security에서 사용자 식별자는 username으로만 접근 가능
        MemberResponse member = memberService.getMemberByUserId(userId);
        return ApiResponse.onSuccess(SuccessStatus.OK, member);
    }

    @Operation(summary = "내 프로필 업데이트", description = "현재 로그인한 사용자의 프로필을 업데이트합니다.")
    @SecurityRequirement(name = "JWT")
    @PutMapping("/me/profile")
    public ApiResponse<MemberResponse> updateMyProfile(
            @Parameter(description = "닉네임") @RequestParam(required = false) String nickname,
            @Parameter(description = "자기소개") @RequestParam(required = false) String description,
            @Parameter(description = "프로필 이미지 URL") @RequestParam(required = false) String profileImageUrl,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername(); // Spring Security username = 우리의 userId
        MemberResponse currentMember = memberService.getMemberByUserId(userId);

        MemberResponse updatedMember = memberService.updateMemberProfile(
                currentMember.getId(), nickname, description, profileImageUrl);

        log.info("Profile updated by user: {}", userId);
        return ApiResponse.onSuccess(SuccessStatus.OK, updatedMember);
    }

    @Operation(summary = "회원 탈퇴", description = "현재 로그인한 사용자가 회원 탈퇴를 진행합니다.")
    @SecurityRequirement(name = "JWT")
    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMember(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername(); // JWT에서 본인 userId 추출
        MemberResponse currentMember = memberService.getMemberByUserId(userId);

        memberService.deleteMember(currentMember.getId());
        log.info("Member self-deleted: {}", userId);
        return ApiResponse.onSuccess(SuccessStatus.OK, null);
    }

    @Operation(summary = "사용자 아이디 중복 확인", description = "사용자 아이디(이메일) 사용 가능 여부를 확인합니다.")
    @GetMapping("/check/userId/{userId}")
    public ApiResponse<Boolean> checkUserIdAvailability(
            @Parameter(description = "확인할 사용자 아이디 (이메일)", required = true) @PathVariable String userId) {
        boolean available = memberService.isUserIdAvailable(userId);
        return ApiResponse.onSuccess(SuccessStatus.OK, available);
    }

    @Operation(summary = "닉네임 중복 확인", description = "닉네임 사용 가능 여부를 확인합니다.")
    @GetMapping("/check/nickname/{nickname}")
    public ApiResponse<Boolean> checkNicknameAvailability(
            @Parameter(description = "확인할 닉네임", required = true) @PathVariable String nickname) {
        boolean available = memberService.isNicknameAvailable(nickname);
        return ApiResponse.onSuccess(SuccessStatus.OK, available);
    }
}