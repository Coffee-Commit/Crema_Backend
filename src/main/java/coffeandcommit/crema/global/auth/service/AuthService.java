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

    public void setTokensToCookie(HttpServletResponse response, String userId) {
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        int accessTokenMaxAge = (int) (jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000);
        int refreshTokenMaxAge = (int) (jwtTokenProvider.getRefreshTokenValidityInMilliseconds() / 1000);

        cookieUtil.addCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE_NAME, accessToken, accessTokenMaxAge);
        cookieUtil.addCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME, refreshToken, refreshTokenMaxAge);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response, String userId) {
        String accessToken = extractAccessToken(request);
        String refreshToken = CookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);

        tokenBlacklistService.blacklistUserTokens(accessToken, refreshToken);

        cookieUtil.deleteCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE_NAME);
        cookieUtil.deleteCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);
    }

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

        String oldAccessToken = extractAccessToken(request);
        tokenBlacklistService.blacklistUserTokens(oldAccessToken, refreshTokenValue);

        String userId = jwtTokenProvider.getUsername(refreshTokenValue);
        String newAccessToken = jwtTokenProvider.createAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

        int accessTokenMaxAge = (int) (jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000);
        int refreshTokenMaxAge = (int) (jwtTokenProvider.getRefreshTokenValidityInMilliseconds() / 1000);

        cookieUtil.addCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE_NAME, newAccessToken, accessTokenMaxAge);
        cookieUtil.addCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME, newRefreshToken, refreshTokenMaxAge);
    }

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

    public String extractAccessToken(HttpServletRequest request) {
        String tokenFromCookie = CookieUtil.getCookie(request, CookieUtil.ACCESS_TOKEN_COOKIE_NAME);
        if (StringUtils.hasText(tokenFromCookie)) {
            return tokenFromCookie;
        }

        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}