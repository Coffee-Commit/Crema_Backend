package coffeandcommit.crema.global.auth.service;

import coffeandcommit.crema.global.auth.jwt.JwtTokenProvider;
import coffeandcommit.crema.global.auth.util.CookieUtil;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * 쿠키에 JWT 토큰 설정
     */
    public void setTokensToCookie(HttpServletResponse response, String userId) {
        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        // 쿠키에 설정
        CookieUtil.addCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE_NAME, accessToken,
                (int) (jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000));

        CookieUtil.addCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME, refreshToken,
                (int) (jwtTokenProvider.getRefreshTokenValidityInMilliseconds() / 1000));

        log.info("JWT tokens set to cookies for user: {}", userId);
    }

    /**
     * 로그아웃 - 쿠키 삭제 및 토큰 블랙리스트 추가
     */
    public void logout(HttpServletRequest request, HttpServletResponse response, String userId) {
        // 현재 토큰들을 블랙리스트에 추가
        String accessToken = extractAccessToken(request);
        String refreshToken = CookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);

        tokenBlacklistService.blacklistUserTokens(accessToken, refreshToken);

        // 쿠키 삭제
        CookieUtil.deleteCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE_NAME);
        CookieUtil.deleteCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);

        log.info("Logout completed for user: {} - cookies cleared and tokens blacklisted", userId);
    }

    /**
     * 토큰 갱신
     */
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = CookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);

        if (!StringUtils.hasText(refreshTokenValue)) {
            throw new BaseException(ErrorStatus.MISSING_TOKEN);
        }

        // 토큰 검증
        if (!jwtTokenProvider.validateToken(refreshTokenValue)) {
            throw new BaseException(ErrorStatus.INVALID_TOKEN);
        }

        if (!jwtTokenProvider.isRefreshToken(refreshTokenValue)) {
            throw new BaseException(ErrorStatus.INVALID_TOKEN);
        }

        // 블랙리스트 확인
        if (tokenBlacklistService.isTokenBlacklisted(refreshTokenValue)) {
            throw new BaseException(ErrorStatus.INVALID_TOKEN);
        }

        // 기존 토큰들을 블랙리스트에 추가
        String oldAccessToken = extractAccessToken(request);
        tokenBlacklistService.blacklistUserTokens(oldAccessToken, refreshTokenValue);

        // 새로운 토큰 생성
        String userId = jwtTokenProvider.getUsername(refreshTokenValue);

        String newAccessToken = jwtTokenProvider.createAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

        // 새로운 토큰을 쿠키에 설정
        CookieUtil.addCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE_NAME, newAccessToken,
                (int) (jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000));

        CookieUtil.addCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME, newRefreshToken,
                (int) (jwtTokenProvider.getRefreshTokenValidityInMilliseconds() / 1000));

        log.info("Tokens refreshed for user: {}", userId);
    }

    /**
     * 현재 로그인 상태 확인
     */
    public boolean isAuthenticated(HttpServletRequest request) {
        String accessToken = extractAccessToken(request);

        if (!StringUtils.hasText(accessToken)) {
            return false;
        }

        // JWT 자체 검증
        if (!jwtTokenProvider.validateToken(accessToken) || !jwtTokenProvider.isAccessToken(accessToken)) {
            return false;
        }

        // 블랙리스트 확인
        return !tokenBlacklistService.isTokenBlacklisted(accessToken);
    }

    /**
     * 요청에서 Access Token 추출 (쿠키 우선, 헤더 백업)
     */
    public String extractAccessToken(HttpServletRequest request) {
        // 1. 쿠키에서 토큰 확인
        String tokenFromCookie = CookieUtil.getCookie(request, CookieUtil.ACCESS_TOKEN_COOKIE_NAME);
        if (StringUtils.hasText(tokenFromCookie)) {
            return tokenFromCookie;
        }

        // 2. Authorization 헤더에서 토큰 확인 (백업용)
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}