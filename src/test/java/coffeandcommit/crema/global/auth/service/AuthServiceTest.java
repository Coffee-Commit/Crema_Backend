package coffeandcommit.crema.global.auth.service;

import coffeandcommit.crema.global.auth.jwt.JwtTokenProvider;
import coffeandcommit.crema.global.auth.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService 테스트 - 자동 토큰 재발급 (String 반환)")
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private final String TEST_MEMBER_ID = "test123";
    private final String OLD_ACCESS_TOKEN = "old.access.token";
    private final String VALID_REFRESH_TOKEN = "valid.refresh.token";
    private final String NEW_ACCESS_TOKEN = "new.access.token";
    private final String NEW_REFRESH_TOKEN = "new.refresh.token";

    @Test
    @DisplayName("자동 재발급 성공 - 새로운 Access Token 반환")
    void attemptAutoRefreshSuccess() {
        // Given
        try (MockedStatic<CookieUtil> mockedCookieUtil = mockStatic(CookieUtil.class)) {
            // Refresh Token 쿠키에서 추출
            mockedCookieUtil.when(() -> CookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME))
                    .thenReturn(VALID_REFRESH_TOKEN);

            // Refresh Token 검증 성공
            given(jwtTokenProvider.validateToken(VALID_REFRESH_TOKEN)).willReturn(true);
            given(jwtTokenProvider.isRefreshToken(VALID_REFRESH_TOKEN)).willReturn(true);
            given(tokenBlacklistService.isTokenBlacklisted(VALID_REFRESH_TOKEN)).willReturn(false);

            // 새로운 토큰 생성
            given(jwtTokenProvider.getMemberId(VALID_REFRESH_TOKEN)).willReturn(TEST_MEMBER_ID);
            given(jwtTokenProvider.createAccessToken(TEST_MEMBER_ID)).willReturn(NEW_ACCESS_TOKEN);
            given(jwtTokenProvider.createRefreshToken(TEST_MEMBER_ID)).willReturn(NEW_REFRESH_TOKEN);
            given(jwtTokenProvider.getAccessTokenValidityInMilliseconds()).willReturn(1800000L); // 30분
            given(jwtTokenProvider.getRefreshTokenValidityInMilliseconds()).willReturn(1209600000L); // 2주

            // When
            String result = authService.attemptAutoRefresh(request, response);

            // Then
            assertThat(result).isEqualTo(NEW_ACCESS_TOKEN); // 새로운 Access Token 반환 확인

            // 새로운 토큰들이 쿠키에 설정되었는지 확인
            then(cookieUtil).should().addCookie(eq(response), eq(CookieUtil.ACCESS_TOKEN_COOKIE_NAME),
                    eq(NEW_ACCESS_TOKEN), eq(1800));
            then(cookieUtil).should().addCookie(eq(response), eq(CookieUtil.REFRESH_TOKEN_COOKIE_NAME),
                    eq(NEW_REFRESH_TOKEN), eq(1209600));

            // 토큰 재발급 헤더가 설정되었는지 확인
            then(response).should().setHeader("X-Token-Refreshed", "true");
        }
    }

    @Test
    @DisplayName("자동 재발급 실패 - Refresh Token이 없는 경우")
    void attemptAutoRefreshFailsWhenNoRefreshToken() {
        // Given
        try (MockedStatic<CookieUtil> mockedCookieUtil = mockStatic(CookieUtil.class)) {
            mockedCookieUtil.when(() -> CookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME))
                    .thenReturn(null);

            // When
            String result = authService.attemptAutoRefresh(request, response);

            // Then
            assertThat(result).isNull(); // 실패시 null 반환
            then(jwtTokenProvider).should(never()).validateToken(anyString());
            then(cookieUtil).should(never()).addCookie(any(), anyString(), anyString(), anyInt());
        }
    }

    @Test
    @DisplayName("자동 재발급 실패 - Refresh Token이 유효하지 않은 경우")
    void attemptAutoRefreshFailsWhenInvalidRefreshToken() {
        // Given
        try (MockedStatic<CookieUtil> mockedCookieUtil = mockStatic(CookieUtil.class)) {
            mockedCookieUtil.when(() -> CookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME))
                    .thenReturn(VALID_REFRESH_TOKEN);

            // Refresh Token 검증 실패
            given(jwtTokenProvider.validateToken(VALID_REFRESH_TOKEN)).willReturn(false);

            // When
            String result = authService.attemptAutoRefresh(request, response);

            // Then
            assertThat(result).isNull(); // 실패시 null 반환
            then(jwtTokenProvider).should(never()).createAccessToken(anyString());
            then(cookieUtil).should(never()).addCookie(any(), anyString(), anyString(), anyInt());
        }
    }

    @Test
    @DisplayName("자동 재발급 실패 - Refresh Token이 블랙리스트된 경우")
    void attemptAutoRefreshFailsWhenBlacklistedRefreshToken() {
        // Given
        try (MockedStatic<CookieUtil> mockedCookieUtil = mockStatic(CookieUtil.class)) {
            mockedCookieUtil.when(() -> CookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME))
                    .thenReturn(VALID_REFRESH_TOKEN);

            // Refresh Token 검증은 성공하지만 블랙리스트됨
            given(jwtTokenProvider.validateToken(VALID_REFRESH_TOKEN)).willReturn(true);
            given(jwtTokenProvider.isRefreshToken(VALID_REFRESH_TOKEN)).willReturn(true);
            given(tokenBlacklistService.isTokenBlacklisted(VALID_REFRESH_TOKEN)).willReturn(true);

            // When
            String result = authService.attemptAutoRefresh(request, response);

            // Then
            assertThat(result).isNull(); // 실패시 null 반환
            then(jwtTokenProvider).should(never()).createAccessToken(anyString());
            then(cookieUtil).should(never()).addCookie(any(), anyString(), anyString(), anyInt());
        }
    }

    @Test
    @DisplayName("자동 재발급 실패 - Refresh Token이 아닌 경우")
    void attemptAutoRefreshFailsWhenNotRefreshToken() {
        // Given
        try (MockedStatic<CookieUtil> mockedCookieUtil = mockStatic(CookieUtil.class)) {
            mockedCookieUtil.when(() -> CookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME))
                    .thenReturn(VALID_REFRESH_TOKEN);

            // 토큰 검증은 성공하지만 Refresh Token이 아님
            given(jwtTokenProvider.validateToken(VALID_REFRESH_TOKEN)).willReturn(true);
            given(jwtTokenProvider.isRefreshToken(VALID_REFRESH_TOKEN)).willReturn(false);

            // When
            String result = authService.attemptAutoRefresh(request, response);

            // Then
            assertThat(result).isNull(); // 실패시 null 반환
            then(jwtTokenProvider).should(never()).createAccessToken(anyString());
            then(cookieUtil).should(never()).addCookie(any(), anyString(), anyString(), anyInt());
        }
    }

    @Test
    @DisplayName("자동 재발급 실패 - 예외 발생 시")
    void attemptAutoRefreshFailsWhenExceptionOccurs() {
        // Given
        try (MockedStatic<CookieUtil> mockedCookieUtil = mockStatic(CookieUtil.class)) {
            mockedCookieUtil.when(() -> CookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME))
                    .thenReturn(VALID_REFRESH_TOKEN);

            // JWT Provider에서 예외 발생
            given(jwtTokenProvider.validateToken(VALID_REFRESH_TOKEN))
                    .willThrow(new RuntimeException("JWT validation error"));

            // When
            String result = authService.attemptAutoRefresh(request, response);

            // Then
            assertThat(result).isNull(); // 예외 발생시 null 반환
            then(cookieUtil).should(never()).addCookie(any(), anyString(), anyString(), anyInt());
        }
    }
}