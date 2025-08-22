package coffeandcommit.crema.global.auth.controller;

import coffeandcommit.crema.domain.member.dto.response.MemberResponse;
import coffeandcommit.crema.domain.member.service.MemberService;
import coffeandcommit.crema.global.auth.jwt.JwtTokenProvider;
import coffeandcommit.crema.global.auth.service.AuthService;
import coffeandcommit.crema.global.common.exception.response.ApiResponse;
import coffeandcommit.crema.global.common.exception.code.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth API", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "현재 로그인 사용자 정보 조회", description = "쿠키의 JWT 토큰을 통해 현재 로그인된 사용자 정보를 조회합니다.")
    @SecurityRequirement(name = "JWT")
    @GetMapping("/me")
    public ApiResponse<MemberResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername();
        MemberResponse member = memberService.getMemberByUserId(userId);
        return ApiResponse.onSuccess(SuccessStatus.OK, member);
    }

    @Operation(summary = "로그아웃", description = "쿠키에서 JWT 토큰을 삭제하여 로그아웃 처리합니다.")
    @SecurityRequirement(name = "JWT")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername();
        authService.logout(response);
        log.info("User logged out: {}", userId);
        return ApiResponse.onSuccess(SuccessStatus.OK, null);
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.")
    @PostMapping("/refresh")
    public ApiResponse<Void> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        authService.refreshToken(request, response);
        return ApiResponse.onSuccess(SuccessStatus.OK, null);
    }

    @Operation(summary = "로그인 상태 확인", description = "현재 로그인 상태를 확인합니다.")
    @GetMapping("/status")
    public ApiResponse<Boolean> checkAuthStatus(HttpServletRequest request) {
        boolean isAuthenticated = authService.isAuthenticated(request);
        return ApiResponse.onSuccess(SuccessStatus.OK, isAuthenticated);
    }
}