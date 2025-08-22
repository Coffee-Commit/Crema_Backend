package coffeandcommit.crema.global.auth.jwt;

import coffeandcommit.crema.global.auth.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;

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
            "/actuator/info"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // 스킵할 경로인지 확인
        if (shouldSkip(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 쿠키 우선, 헤더 백업으로 토큰 추출
            String token = authService.extractAccessToken(request);

            if (StringUtils.hasText(token)) {
                if (jwtTokenProvider.validateToken(token) && jwtTokenProvider.isAccessToken(token)) {
                    setAuthentication(token, request);
                    log.debug("JWT authentication successful for URI: {}", requestURI);
                } else {
                    log.debug("Invalid JWT token for URI: {}", requestURI);
                    SecurityContextHolder.clearContext();
                }
            } else {
                log.debug("No JWT token found in request for URI: {}", requestURI);
            }
        } catch (Exception e) {
            log.error("JWT authentication failed for URI: {} - {}", requestURI, e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkip(String requestURI) {
        return SKIP_PATHS.stream().anyMatch(requestURI::startsWith);
    }

    private void setAuthentication(String token, HttpServletRequest request) {
        try {
            String username = jwtTokenProvider.getUsername(token);
            String role = jwtTokenProvider.getRole(token);

            if (StringUtils.hasText(username) && StringUtils.hasText(role)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Set authentication for user: {} with role: {}", username, role);
            }
        } catch (Exception e) {
            log.error("Failed to set authentication: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }
}