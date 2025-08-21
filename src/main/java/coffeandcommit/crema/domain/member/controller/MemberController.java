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

    @Operation(summary = "회원 정보 조회 (ID)", description = "회원 ID로 회원 정보를 조회합니다.")
    @SecurityRequirement(name = "JWT")
    @GetMapping("/{id}")
    public ApiResponse<MemberResponse> getMember(
            @Parameter(description = "회원 ID", required = true) @PathVariable String id) {
        MemberResponse member = memberService.getMemberById(id);
        return ApiResponse.onSuccess(SuccessStatus.OK, member);
    }

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

    @Operation(summary = "프로필 업데이트", description = "회원 프로필 정보를 업데이트합니다.")
    @SecurityRequirement(name = "JWT")
    @PutMapping("/{id}/profile")
    public ApiResponse<MemberResponse> updateProfile(
            @Parameter(description = "회원 ID", required = true) @PathVariable String id,
            @Parameter(description = "닉네임") @RequestParam(required = false) String nickname,
            @Parameter(description = "자기소개") @RequestParam(required = false) String description,
            @Parameter(description = "프로필 이미지 URL") @RequestParam(required = false) String profileImageUrl,
            @AuthenticationPrincipal UserDetails userDetails) {

        // 본인만 프로필 수정 가능하도록 검증 (또는 관리자)
        // 실제로는 userDetails에서 user ID를 가져와야 함

        MemberResponse member = memberService.updateMemberProfile(id, nickname, description, profileImageUrl);
        log.info("Profile updated by user: {} for member: {}", userDetails.getUsername(), id);
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

    @Operation(summary = "회원 탈퇴", description = "회원을 삭제합니다. (관리자 전용)")
    @SecurityRequirement(name = "JWT")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMember(
            @Parameter(description = "회원 ID", required = true) @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        memberService.deleteMember(id);
        log.info("Member deleted by admin: {} for member: {}", userDetails.getUsername(), id);
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

    @Operation(summary = "내 포인트 조회", description = "현재 로그인한 사용자의 포인트를 조회합니다.")
    @SecurityRequirement(name = "JWT")
    @GetMapping("/me/point")
    public ApiResponse<Integer> getMyPoints(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername(); // Spring Security username = 우리의 userId
        MemberResponse member = memberService.getMemberByUserId(userId);
        int point = memberService.getMemberPoints(member.getId());
        return ApiResponse.onSuccess(SuccessStatus.OK, point);
    }

    @Operation(summary = "포인트 추가", description = "회원에게 포인트를 추가합니다. (관리자 전용)")
    @SecurityRequirement(name = "JWT")
    @PostMapping("/{id}/point/add")
    public ApiResponse<Void> addPoints(
            @Parameter(description = "회원 ID", required = true) @PathVariable String id,
            @Parameter(description = "추가할 포인트", required = true) @RequestParam int point,
            @AuthenticationPrincipal UserDetails userDetails) {

        memberService.addPoints(id, point);
        log.info("Points added by admin: {} to member: {} amount: {}",
                userDetails.getUsername(), id, point);
        return ApiResponse.onSuccess(SuccessStatus.OK, null);
    }

    @Operation(summary = "포인트 차감", description = "회원의 포인트를 차감합니다. (관리자 전용)")
    @SecurityRequirement(name = "JWT")
    @PostMapping("/{id}/point/decrease")
    public ApiResponse<Void> decreasePoints(
            @Parameter(description = "회원 ID", required = true) @PathVariable String id,
            @Parameter(description = "차감할 포인트", required = true) @RequestParam int point,
            @AuthenticationPrincipal UserDetails userDetails) {

        memberService.decreasePoints(id, point);
        log.info("Points decreased by admin: {} from member: {} amount: {}",
                userDetails.getUsername(), id, point);
        return ApiResponse.onSuccess(SuccessStatus.OK, null);
    }
}