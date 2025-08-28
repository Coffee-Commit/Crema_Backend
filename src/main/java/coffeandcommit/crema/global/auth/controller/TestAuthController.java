package coffeandcommit.crema.global.auth.controller;

import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.enums.MemberRole;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import coffeandcommit.crema.domain.member.service.MemberService;
import coffeandcommit.crema.global.auth.jwt.JwtTokenProvider;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import coffeandcommit.crema.global.common.exception.response.ApiResponse;
import coffeandcommit.crema.global.common.exception.code.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/test/auth")
@RequiredArgsConstructor
@Profile("local")
@Tag(name = "Test Auth API", description = "로컬 개발용 테스트 인증 API (local 프로필에서만 활성화)")
public class TestAuthController {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberService memberService; // MemberService 주입 추가

    @Operation(summary = "루키 테스트 계정 생성", description = "로컬 개발용 루키 테스트 계정을 생성합니다.")
    @PostMapping("/create-rookie")
    public ApiResponse<Map<String, String>> createRookieAccount() {
        String nickname = generateNickname("rookie");
        Member member = createTestMember(nickname, MemberRole.ROOKIE);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("memberId", member.getId());
        response.put("nickname", member.getNickname());
        response.put("role", member.getRole().name());

        log.info("테스트 루키 계정 생성: {}", nickname);
        return ApiResponse.onSuccess(SuccessStatus.CREATED, response);
    }

    @Operation(summary = "가이드 테스트 계정 생성", description = "로컬 개발용 가이드 테스트 계정을 생성합니다.")
    @PostMapping("/create-guide")
    public ApiResponse<Map<String, String>> createGuideAccount() {
        String nickname = generateNickname("guide");
        Member member = createTestMember(nickname, MemberRole.GUIDE);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("memberId", member.getId());
        response.put("nickname", member.getNickname());
        response.put("role", member.getRole().name());

        log.info("테스트 가이드 계정 생성: {}", nickname);
        return ApiResponse.onSuccess(SuccessStatus.CREATED, response);
    }

    @Operation(summary = "테스트 계정 로그인", description = "생성된 테스트 계정으로 로그인하여 JWT 토큰을 발급받습니다.")
    @PostMapping("/login")
    public ApiResponse<Map<String, String>> loginTestAccount(
            @Parameter(description = "테스트 계정 닉네임 (예: rookie_12345678, guide_12345678)", required = true)
            @RequestBody Map<String, String> request) {

        String nickname = request.get("nickname");

        if (nickname == null || nickname.trim().isEmpty()) {
            throw new BaseException(ErrorStatus.BAD_REQUEST);
        }

        if (!isTestAccount(nickname)) {
            throw new BaseException(ErrorStatus.BAD_REQUEST);
        }

        Member member = memberRepository.findByNicknameAndIsDeletedFalse(nickname)
                .orElseThrow(() -> new BaseException(ErrorStatus.MEMBER_NOT_FOUND));

        String accessToken = jwtTokenProvider.createAccessToken(member.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        Map<String, String> response = new LinkedHashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("memberId", member.getId());
        response.put("nickname", member.getNickname());
        response.put("role", member.getRole().name());
        response.put("tokenType", "Bearer");

        log.info("테스트 계정 로그인: {}", nickname);
        return ApiResponse.onSuccess(SuccessStatus.OK, response);
    }

    @Operation(summary = "테스트 계정 일괄 삭제", description = "생성된 모든 테스트 계정을 완전 삭제합니다.")
    @DeleteMapping("/cleanup")
    public ApiResponse<Map<String, Object>> cleanupTestAccounts() {
        int deletedCount = memberRepository.deleteTestAccountsNative();

        log.info("테스트 계정 완전 삭제 완료: {}개", deletedCount);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("deletedCount", deletedCount);
        response.put("message", deletedCount + "개의 테스트 계정이 완전 삭제되었습니다.");

        return ApiResponse.onSuccess(SuccessStatus.OK, response);
    }

    private String generateNickname(String rolePrefix) {
        for (int i = 0; i < 5; i++) {
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            String nickname = rolePrefix + "_" + uuid;

            if (!memberRepository.existsByNicknameAndIsDeletedFalse(nickname)) {
                return nickname;
            }
        }

        throw new BaseException(ErrorStatus.INTERNAL_SERVER_ERROR);
    }

    private Member createTestMember(String nickname, MemberRole role) {
        try {
            // MemberService의 generateId() 메서드 사용
            String memberId = memberService.generateId();

            Member member = Member.builder()
                    .id(memberId) // 8글자 고유 ID 사용
                    .nickname(nickname)
                    .role(role)
                    .point(0)
                    .provider("test")
                    .providerId(nickname)
                    .build();

            return memberRepository.saveAndFlush(member);

        } catch (Exception e) {
            log.warn("테스트 계정 생성 실패, 닉네임 재생성 후 재시도: {}", nickname);
            String newNickname = generateNickname(role == MemberRole.ROOKIE ? "rookie" : "guide");

            // 재시도 시에도 MemberService의 generateId() 사용
            String memberId = memberService.generateId();

            Member member = Member.builder()
                    .id(memberId) // 8글자 고유 ID 사용
                    .nickname(newNickname)
                    .role(role)
                    .point(0)
                    .provider("test")
                    .providerId(newNickname)
                    .build();

            return memberRepository.save(member);
        }
    }

    private boolean isTestAccount(String nickname) {
        return nickname != null &&
                (nickname.startsWith("rookie_") || nickname.startsWith("guide_"));
    }
}