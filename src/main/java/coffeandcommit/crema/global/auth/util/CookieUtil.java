package coffeandcommit.crema.global.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Optional;

@Component
public class CookieUtil {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    private static String domain;
    private static boolean isProduction;

    @Value("${app.cookie.domain:}")
    public void setDomain(String domain) {
        CookieUtil.domain = domain;
    }

    @Value("${spring.profiles.active:dev}")
    public void setActiveProfile(String profile) {
        CookieUtil.isProduction = "prod".equals(profile) || "production".equals(profile);
    }

    /**
     * 쿠키 추가
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);

        // 운영환경에서는 Secure 설정
        if (isProduction) {
            cookie.setSecure(true);
        }

        // SameSite 설정을 위한 헤더 직접 설정
        String cookieHeader = createCookieHeader(name, value, maxAge);
        response.addHeader("Set-Cookie", cookieHeader);
    }

    /**
     * 쿠키 삭제
     */
    public static void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);

        if (isProduction) {
            cookie.setSecure(true);
        }

        // SameSite 설정을 위한 헤더 직접 설정
        String cookieHeader = createDeleteCookieHeader(name);
        response.addHeader("Set-Cookie", cookieHeader);
    }

    /**
     * 쿠키 값 조회
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
     * SameSite 속성을 포함한 쿠키 헤더 생성
     */
    private static String createCookieHeader(String name, String value, int maxAge) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value);
        sb.append("; Path=/");
        sb.append("; HttpOnly");
        sb.append("; Max-Age=").append(maxAge);
        sb.append("; SameSite=Strict");

        if (isProduction) {
            sb.append("; Secure");
        }

        if (StringUtils.hasText(domain)) {
            sb.append("; Domain=").append(domain);
        }

        return sb.toString();
    }

    /**
     * 쿠키 삭제용 헤더 생성
     */
    private static String createDeleteCookieHeader(String name) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=");
        sb.append("; Path=/");
        sb.append("; HttpOnly");
        sb.append("; Max-Age=0");
        sb.append("; SameSite=Strict");

        if (isProduction) {
            sb.append("; Secure");
        }

        if (StringUtils.hasText(domain)) {
            sb.append("; Domain=").append(domain);
        }

        return sb.toString();
    }
}