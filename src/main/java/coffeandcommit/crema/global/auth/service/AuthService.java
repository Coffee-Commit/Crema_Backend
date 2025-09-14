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
     * Refresh Token을 이용한 토큰 갱신 (수동 재발급 - 컨트롤러용)
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
     * 자동 토큰 재발급 (필터에서 사용 - 예외 대신 boolean 반환)
     */
    public boolean attemptAutoRefresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshTokenValue = CookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);

            if (!StringUtils.hasText(refreshTokenValue)) {
                log.debug("No refresh token found for auto-refresh");
                return false;
            }

            if (!jwtTokenProvider.validateToken(refreshTokenValue)) {
                log.debug("Refresh token is invalid");
                return false;
            }

            if (!jwtTokenProvider.isRefreshToken(refreshTokenValue)) {
                log.debug("Token is not a refresh token");
                return false;
            }

            if (tokenBlacklistService.isTokenBlacklisted(refreshTokenValue)) {
                log.debug("Refresh token is blacklisted");
                return false;
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

            // 프론트엔드에게 토큰이 재발급되었음을 알리는 헤더 추가
            response.setHeader("X-Token-Refreshed", "true");

            log.info("Token auto-refresh successful for member: {}", memberId);
            return true;

        } catch (Exception e) {
            log.error("Auto-refresh failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 토큰 재발급 후 새로운 Access Token으로 인증 컨텍스트 재설정을 위한 헬퍼 메서드
     */
    public String getRefreshedAccessToken(HttpServletRequest request) {
        return extractAccessToken(request);
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
        log.debug("Extracting access token from request: {}", request.getRequestURI());

        // 1. 쿠키에서 토큰 추출 시도
        String tokenFromCookie = CookieUtil.getCookie(request, CookieUtil.ACCESS_TOKEN_COOKIE_NAME);
        log.debug("Token from cookie: {}", tokenFromCookie != null ? "Present (length: " + tokenFromCookie.length() + ")" : "Not found");

        if (StringUtils.hasText(tokenFromCookie)) {
            log.debug("Using token from cookie");
            return tokenFromCookie;
        }

        // 2. Authorization 헤더에서 Bearer 토큰 추출 시도
        String bearerToken = request.getHeader("Authorization");
        log.debug("Authorization header: {}", bearerToken != null ? "Present" : "Not found");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            log.debug("Using token from Authorization header (length: {})", token.length());
            return token;
        }

        log.debug("No access token found in request");
        return null;
    }
}