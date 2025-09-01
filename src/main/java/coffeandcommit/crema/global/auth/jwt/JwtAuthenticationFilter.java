package coffeandcommit.crema.global.auth.jwt;

import coffeandcommit.crema.domain.member.service.MemberService;
import coffeandcommit.crema.global.auth.service.AuthService;
import coffeandcommit.crema.global.auth.service.CustomUserDetails;
import coffeandcommit.crema.global.auth.service.TokenBlacklistService;
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
            String token = authService.extractAccessToken(request);
            log.debug("Extracted token: {}", token != null ? "Present" : "Not found");

            if (StringUtils.hasText(token)) {
                log.debug("Token found, validating...");

                // JWT 토큰 검증
                if (jwtTokenProvider.validateToken(token) && jwtTokenProvider.isAccessToken(token)) {
                    log.debug("Token is valid and is access token");

                    // 블랙리스트 확인
                    if (!tokenBlacklistService.isTokenBlacklisted(token)) {
                        log.debug("Token is not blacklisted, setting authentication");
                        setAuthentication(token, request);
                        log.info("JWT authentication successful for URI: {} with member: {}",
                                requestURI, jwtTokenProvider.getMemberId(token));
                    } else {
                        log.warn("Blacklisted token used for URI: {}", requestURI);
                        SecurityContextHolder.clearContext();
                    }
                } else {
                    log.warn("Invalid JWT token for URI: {}", requestURI);
                    SecurityContextHolder.clearContext();
                }
            } else {
                log.debug("No JWT token found in request for URI: {}", requestURI);
            }
        } catch (Exception e) {
            log.error("JWT authentication failed for URI: {} - {}", requestURI, e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
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