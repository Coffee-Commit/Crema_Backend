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
    private final CookieUtil cookieUtil;

    /**
     * JWT 토큰을 생성하고 쿠키에 설정
     */
    public void setTokensToCookie(HttpServletResponse response, String memberId) {
        String accessToken = jwtTokenProvider.createAccessToken(memberId);
        String refreshToken = jwtTokenProvider.createRefreshToken(memberId);

        int accessTokenMaxAge = (int) (jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000);
        int refreshTokenMaxAge = (int) (jwtTokenProvider.getRefreshTokenValidityInMilliseconds() / 1000);

        cookieUtil.addCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE_NAME, accessToken, accessTokenMaxAge);
        cookieUtil.addCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME, refreshToken, refreshTokenMaxAge);

        log.info("Tokens set to cookies for member: {}", memberId);
    }

    /**
     * 로그아웃 처리 - 토큰 블랙리스트 추가 및 쿠키 삭제
     */
    public void logout(HttpServletRequest request, HttpServletResponse response, String memberId) {
        String accessToken = extractAccessToken(request);
        String refreshToken = CookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);

        // 토큰들을 블랙리스트에 추가
        tokenBlacklistService.blacklistUserTokens(accessToken, refreshToken);

        // 쿠키에서 토큰 삭제
        cookieUtil.deleteCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE_NAME);
        cookieUtil.deleteCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);

        log.info("Member logged out: {}", memberId);
    }

    /**
     * Refresh Token을 이용한 토큰 갱신
     */
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = CookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);

        if (!StringUtils.hasText(refreshTokenValue)) {
            throw new BaseException(ErrorStatus.MISSING_TOKEN);
        }

        if (!jwtTokenProvider.validateToken(refreshTokenValue)) {
            throw new BaseException(ErrorStatus.INVALID_TOKEN);
        }

        if (!jwtTokenProvider.isRefreshToken(refreshTokenValue)) {
            throw new BaseException(ErrorStatus.INVALID_TOKEN);
        }

        if (tokenBlacklistService.isTokenBlacklisted(refreshTokenValue)) {
            throw new BaseException(ErrorStatus.INVALID_TOKEN);
        }

        // 기존 토큰들을 블랙리스트에 추가
        String oldAccessToken = extractAccessToken(request);
        tokenBlacklistService.blacklistUserTokens(oldAccessToken, refreshTokenValue);

        // 새로운 토큰 생성
        String memberId = jwtTokenProvider.getMemberId(refreshTokenValue);
        String newAccessToken = jwtTokenProvider.createAccessToken(memberId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(memberId);

        int accessTokenMaxAge = (int) (jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000);
        int refreshTokenMaxAge = (int) (jwtTokenProvider.getRefreshTokenValidityInMilliseconds() / 1000);

        // 새로운 토큰을 쿠키에 설정
        cookieUtil.addCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE_NAME, newAccessToken, accessTokenMaxAge);
        cookieUtil.addCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME, newRefreshToken, refreshTokenMaxAge);

        log.info("Tokens refreshed for member: {}", memberId);
    }

    /**
     * 인증 상태 확인
     */
    public boolean isAuthenticated(HttpServletRequest request) {
        String accessToken = extractAccessToken(request);

        if (!StringUtils.hasText(accessToken)) {
            return false;
        }

        if (!jwtTokenProvider.validateToken(accessToken) || !jwtTokenProvider.isAccessToken(accessToken)) {
            return false;
        }

        return !tokenBlacklistService.isTokenBlacklisted(accessToken);
    }

    /**
     * 요청에서 Access Token 추출 (쿠키 우선, 헤더 백업)
     */
    public String extractAccessToken(HttpServletRequest request) {
        // 1. 쿠키에서 토큰 추출 시도
        String tokenFromCookie = CookieUtil.getCookie(request, CookieUtil.ACCESS_TOKEN_COOKIE_NAME);
        if (StringUtils.hasText(tokenFromCookie)) {
            return tokenFromCookie;
        }

        // 2. Authorization 헤더에서 Bearer 토큰 추출 시도
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}