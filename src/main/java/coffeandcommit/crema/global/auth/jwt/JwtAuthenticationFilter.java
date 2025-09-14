package coffeandcommit.crema.global.auth.jwt;

import coffeandcommit.crema.domain.member.service.MemberService;
import coffeandcommit.crema.global.auth.service.AuthService;
import coffeandcommit.crema.global.auth.service.CustomUserDetails;
import coffeandcommit.crema.global.auth.service.TokenBlacklistService;
import coffeandcommit.crema.global.auth.util.CookieUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;
    private final MemberService memberService;
    private final CookieUtil cookieUtil;

    // JWT 검증을 스킵할 경로들
    private static final List<String> SKIP_PATHS = List.of(
            "/api/auth/status",
            "/api/auth/refresh",
            "/api/member/check",
            "/oauth2",
            "/login/oauth2",
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources",
            "/webjars",
            "/actuator/health",
            "/actuator/info",
            "/after-login"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug("Processing request: {} {}", request.getMethod(), requestURI);

        // 스킵할 경로인지 확인
        if (shouldSkip(requestURI)) {
            log.debug("Skipping JWT authentication for URI: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 쿠키 우선, 헤더 백업으로 토큰 추출
            String accessToken = authService.extractAccessToken(request);
            log.debug("Extracted access token: {}", accessToken != null ? "Present" : "Not found");

            if (StringUtils.hasText(accessToken)) {
                // Access Token 검증
                if (jwtTokenProvider.validateToken(accessToken) && jwtTokenProvider.isAccessToken(accessToken)) {
                    // 블랙리스트 확인
                    if (!tokenBlacklistService.isTokenBlacklisted(accessToken)) {
                        log.debug("Valid access token, setting authentication");
                        setAuthentication(accessToken, request);
                        log.debug("JWT authentication successful for URI: {} with member: {}",
                                requestURI, jwtTokenProvider.getMemberId(accessToken));
                    } else {
                        log.warn("Blacklisted access token used for URI: {}", requestURI);
                        SecurityContextHolder.clearContext();
                    }
                } else {
                    // Access Token이 만료되었거나 유효하지 않음 -> 자동 갱신 시도
                    log.info("Access token invalid/expired for URI: {}, attempting auto-refresh", requestURI);

                    if (attemptTokenRefresh(request, response)) {
                        // 토큰 갱신 성공 -> 새 Access Token으로 인증 설정
                        String newAccessToken = authService.extractAccessToken(request);
                        if (StringUtils.hasText(newAccessToken)) {
                            setAuthentication(newAccessToken, request);
                            log.info("Token auto-refresh successful for URI: {} with member: {}",
                                    requestURI, jwtTokenProvider.getMemberId(newAccessToken));
                        }
                    } else {
                        // 토큰 갱신 실패 -> 인증 실패
                        log.warn("Token auto-refresh failed for URI: {}", requestURI);
                        SecurityContextHolder.clearContext();
                    }
                }
            } else {
                log.debug("No access token found in request for URI: {}", requestURI);
            }
        } catch (Exception e) {
            log.error("JWT authentication failed for URI: {} - {}", requestURI, e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Refresh Token을 이용한 자동 토큰 갱신 시도
     */
    private boolean attemptTokenRefresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            // CookieUtil.getCookie()는 static 메서드이므로 직접 호출
            String refreshToken = CookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);

            if (!StringUtils.hasText(refreshToken)) {
                log.debug("No refresh token found in cookie");
                return false;
            }

            // Refresh Token 검증
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                log.debug("Refresh token is invalid or expired");
                return false;
            }

            if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
                log.debug("Token is not a refresh token");
                return false;
            }

            // 블랙리스트 확인
            if (tokenBlacklistService.isTokenBlacklisted(refreshToken)) {
                log.debug("Refresh token is blacklisted");
                return false;
            }

            // 기존 토큰들을 블랙리스트에 추가
            String oldAccessToken = authService.extractAccessToken(request);
            tokenBlacklistService.blacklistUserTokens(oldAccessToken, refreshToken);

            // 새로운 토큰 생성
            String memberId = jwtTokenProvider.getMemberId(refreshToken);
            String newAccessToken = jwtTokenProvider.createAccessToken(memberId);
            String newRefreshToken = jwtTokenProvider.createRefreshToken(memberId);

            // 토큰 유효시간 계산
            int accessTokenMaxAge = (int) (jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000);
            int refreshTokenMaxAge = (int) (jwtTokenProvider.getRefreshTokenValidityInMilliseconds() / 1000);

            // 새로운 토큰을 쿠키에 설정
            cookieUtil.addCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE_NAME, newAccessToken, accessTokenMaxAge);
            cookieUtil.addCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME, newRefreshToken, refreshTokenMaxAge);

            log.info("Token auto-refresh successful for member: {}", memberId);
            return true;

        } catch (Exception e) {
            log.error("Failed to auto-refresh token: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean shouldSkip(String requestURI) {
        boolean skip = SKIP_PATHS.stream().anyMatch(requestURI::startsWith);
        log.debug("Should skip URI {}: {}", requestURI, skip);
        return skip;
    }

    private void setAuthentication(String token, HttpServletRequest request) {
        try {
            String memberId = jwtTokenProvider.getMemberId(token);

            if (StringUtils.hasText(memberId)) {
                // DB 조회 또는 토큰 클레임으로 사용자 상태 확인 (enabled) 조회
                CustomUserDetails userDetails = memberService.createUserDetails(memberId);

                // 사용자가 비활성 상태면 인증 실패
                if (!userDetails.isEnabled()) {
                    log.warn("User account is disabled: {}", memberId);
                    SecurityContextHolder.clearContext();
                    return;
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.error("Failed to set authentication: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }
}