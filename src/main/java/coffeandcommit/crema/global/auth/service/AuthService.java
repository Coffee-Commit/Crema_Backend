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

    /**
     * 쿠키에 JWT 토큰 설정
     */
    public void setTokensToCookie(HttpServletResponse response, String accessToken, String refreshToken) {
        // Access Token 쿠키 설정 (30분)
        CookieUtil.addCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE_NAME, accessToken,
                (int) (jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000));

        // Refresh Token 쿠키 설정 (14일)
        CookieUtil.addCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME, refreshToken,
                (int) (jwtTokenProvider.getRefreshTokenValidityInMilliseconds() / 1000));

        log.info("JWT tokens set to cookies successfully");
    }

    /**
     * 로그아웃 - 쿠키에서 토큰 삭제
     */
    public void logout(HttpServletResponse response) {
        CookieUtil.deleteCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE_NAME);
        CookieUtil.deleteCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);
        log.info("Logout completed - cookies cleared");
    }

    /**
     * 토큰 갱신
     */
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = CookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);

        if (!StringUtils.hasText(refreshToken)) {
            throw new BaseException(ErrorStatus.MISSING_TOKEN);
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BaseException(ErrorStatus.INVALID_TOKEN);
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BaseException(ErrorStatus.INVALID_TOKEN);
        }

        // 기존 토큰에서 사용자 정보 추출
        String userId = jwtTokenProvider.getUsername(refreshToken);

        // 새로운 Access Token 생성 (기존 Refresh Token의 role 정보 필요)
        // Refresh Token에는 role 정보가 없으므로, 사용자 정보를 다시 조회해야 함
        // 여기서는 간단히 ROOKIE로 설정하지만, 실제로는 DB에서 조회해야 함
        String newAccessToken = jwtTokenProvider.createAccessToken(userId, "ROOKIE"); // TODO: DB에서 실제 role 조회
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

        // 새로운 토큰을 쿠키에 설정
        setTokensToCookie(response, newAccessToken, newRefreshToken);

        log.info("Tokens refreshed for user: {}", userId);
    }

    /**
     * 현재 로그인 상태 확인
     */
    public boolean isAuthenticated(HttpServletRequest request) {
        String accessToken = CookieUtil.getCookie(request, CookieUtil.ACCESS_TOKEN_COOKIE_NAME);

        if (!StringUtils.hasText(accessToken)) {
            return false;
        }

        return jwtTokenProvider.validateToken(accessToken) && jwtTokenProvider.isAccessToken(accessToken);
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