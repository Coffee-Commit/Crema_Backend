package coffeandcommit.crema.global.auth.jwt;

import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.enums.MemberRole;
import coffeandcommit.crema.domain.member.service.MemberService;
import coffeandcommit.crema.global.auth.service.AuthService;
import coffeandcommit.crema.global.auth.service.CustomUserDetails;
import coffeandcommit.crema.global.auth.service.TokenBlacklistService;
import coffeandcommit.crema.global.auth.util.CookieUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Strict stubbing 완화
@DisplayName("JWT 인증 필터 테스트 - 자동 토큰 재발급")
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthService authService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private MemberService memberService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private final String TEST_MEMBER_ID = "test123";
    private final String EXPIRED_ACCESS_TOKEN = "expired.access.token";
    private final String VALID_REFRESH_TOKEN = "valid.refresh.token";
    private final String NEW_ACCESS_TOKEN = "new.access.token";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Access Token이 만료되었을 때 자동 재발급이 성공하는 경우")
    void autoRefreshSuccess() throws Exception {
        // Given
        given(request.getRequestURI()).willReturn("/api/member/me");
        given(authService.extractAccessToken(request)).willReturn(EXPIRED_ACCESS_TOKEN);

        // Access Token 검증 실패 (만료됨)
        given(jwtTokenProvider.validateToken(EXPIRED_ACCESS_TOKEN)).willReturn(false);
        given(jwtTokenProvider.isAccessToken(EXPIRED_ACCESS_TOKEN)).willReturn(true);

        // 자동 재발급 성공 - 새로운 토큰 반환
        given(authService.attemptAutoRefresh(request, response)).willReturn(NEW_ACCESS_TOKEN);

        // 새 토큰으로 인증 설정
        given(jwtTokenProvider.getMemberId(NEW_ACCESS_TOKEN)).willReturn(TEST_MEMBER_ID);
        CustomUserDetails userDetails = new CustomUserDetails(TEST_MEMBER_ID, true, MemberRole.ROOKIE);
        given(memberService.createUserDetails(TEST_MEMBER_ID)).willReturn(userDetails);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        then(authService).should().attemptAutoRefresh(request, response);
        then(memberService).should().createUserDetails(TEST_MEMBER_ID);
        then(filterChain).should().doFilter(request, response);

        // 인증 컨텍스트가 설정되었는지 확인
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(TEST_MEMBER_ID);
    }

    @Test
    @DisplayName("Access Token이 만료되었지만 자동 재발급이 실패하는 경우")
    void autoRefreshFailure() throws Exception {
        // Given
        given(request.getRequestURI()).willReturn("/api/member/me");
        given(authService.extractAccessToken(request)).willReturn(EXPIRED_ACCESS_TOKEN);

        // Access Token 검증 실패 (만료됨)
        given(jwtTokenProvider.validateToken(EXPIRED_ACCESS_TOKEN)).willReturn(false);
        given(jwtTokenProvider.isAccessToken(EXPIRED_ACCESS_TOKEN)).willReturn(true);

        // 자동 재발급 실패 - null 반환
        given(authService.attemptAutoRefresh(request, response)).willReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        then(authService).should().attemptAutoRefresh(request, response);
        then(memberService).should(never()).createUserDetails(anyString());
        then(filterChain).should().doFilter(request, response);

        // 인증 컨텍스트가 클리어되었는지 확인
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("유효한 Access Token인 경우 자동 재발급을 시도하지 않음")
    void validTokenSkipsAutoRefresh() throws Exception {
        // Given
        String validAccessToken = "valid.access.token";
        given(request.getRequestURI()).willReturn("/api/member/me");
        given(authService.extractAccessToken(request)).willReturn(validAccessToken);

        // Access Token 검증 성공
        given(jwtTokenProvider.validateToken(validAccessToken)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(validAccessToken)).willReturn(true);
        given(tokenBlacklistService.isTokenBlacklisted(validAccessToken)).willReturn(false);

        given(jwtTokenProvider.getMemberId(validAccessToken)).willReturn(TEST_MEMBER_ID);

        // 사용자 정보 생성
        CustomUserDetails userDetails = new CustomUserDetails(TEST_MEMBER_ID, true, MemberRole.ROOKIE);
        given(memberService.createUserDetails(TEST_MEMBER_ID)).willReturn(userDetails);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        then(authService).should(never()).attemptAutoRefresh(any(), any());
        then(memberService).should().createUserDetails(TEST_MEMBER_ID);
        then(filterChain).should().doFilter(request, response);

        // 인증 컨텍스트가 설정되었는지 확인
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(TEST_MEMBER_ID);
    }

    @Test
    @DisplayName("스킵 경로인 경우 JWT 처리를 하지 않음")
    void skipPathsBypassesJwtProcessing() throws Exception {
        // Given
        given(request.getRequestURI()).willReturn("/api/auth/status");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        then(authService).should(never()).extractAccessToken(any());
        then(authService).should(never()).attemptAutoRefresh(any(), any());
        then(filterChain).should().doFilter(request, response);
    }

    @Test
    @DisplayName("토큰이 없는 경우 자동 재발급을 시도하지 않음")
    void noTokenSkipsAutoRefresh() throws Exception {
        // Given
        given(request.getRequestURI()).willReturn("/api/member/me");
        given(authService.extractAccessToken(request)).willReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        then(authService).should(never()).attemptAutoRefresh(any(), any());
        then(filterChain).should().doFilter(request, response);

        // 인증 컨텍스트가 없는지 확인
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("블랙리스트된 토큰인 경우 자동 재발급을 시도하지 않음")
    void blacklistedTokenSkipsAutoRefresh() throws Exception {
        // Given
        String blacklistedToken = "blacklisted.access.token";
        given(request.getRequestURI()).willReturn("/api/member/me");
        given(authService.extractAccessToken(request)).willReturn(blacklistedToken);

        // 토큰 검증은 성공하지만 블랙리스트됨
        given(jwtTokenProvider.validateToken(blacklistedToken)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(blacklistedToken)).willReturn(true);
        given(tokenBlacklistService.isTokenBlacklisted(blacklistedToken)).willReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        then(authService).should(never()).attemptAutoRefresh(any(), any());
        then(filterChain).should().doFilter(request, response);

        // 인증 컨텍스트가 클리어되었는지 확인
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}