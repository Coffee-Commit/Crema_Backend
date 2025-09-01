package coffeandcommit.crema.global.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Slf4j
@Component
public class CookieUtil {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    private final String domain;
    private final boolean isProduction;
    private final String sameSite;
    private final boolean isSecure;

    public CookieUtil(
            @Value("${app.cookie.domain:}") String domain,
            @Value("${spring.profiles.active:dev}") String profile,
            @Value("${app.cookie.same-site:Lax}") String sameSite) {
        this.domain = domain;
        this.isProduction = "prod".equals(profile) || "production".equals(profile);
        this.sameSite = sameSite;
        // HTTPS 환경이거나 SameSite=None인 경우 Secure 플래그 필요
        this.isSecure = isProduction || "None".equalsIgnoreCase(sameSite) || "dev".equals(profile);

        log.info("CookieUtil initialized - domain: {}, profile: {}, sameSite: {}, secure: {}",
                domain, profile, sameSite, isSecure);
    }

    /**
     * 쿠키 추가 (ResponseCookie 방식)
     */
    public void addCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        ResponseCookie cookie = createCookie(name, value, maxAgeSeconds);
        response.addHeader("Set-Cookie", cookie.toString());
        log.debug("Cookie added: {} (domain: {}, sameSite: {}, secure: {})", name, domain, sameSite, isSecure);
    }

    /**
     * 쿠키 삭제 (ResponseCookie 방식)
     */
    public void deleteCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = createCookie(name, "", 0);
        response.addHeader("Set-Cookie", cookie.toString());
        log.debug("Cookie deleted: {}", name);
    }

    /**
     * 쿠키 값 조회 (static 유지 - 조회만 하므로 설정값 불필요)
     */
    public static String getCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

    /**
     * ResponseCookie 생성 헬퍼 메서드
     */
    private ResponseCookie createCookie(String name, String value, int maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(sameSite)
                .secure(isSecure);

        // 도메인이 설정되어 있을 때만 추가 (localhost에서는 도메인 설정하면 안됨)
        if (StringUtils.hasText(domain)) {
            cookieBuilder.domain(domain);
        }

        return cookieBuilder.build();
    }
}