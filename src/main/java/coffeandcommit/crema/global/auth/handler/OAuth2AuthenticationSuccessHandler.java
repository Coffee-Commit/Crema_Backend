package coffeandcommit.crema.global.auth.handler;

import coffeandcommit.crema.global.auth.service.AuthService;
import coffeandcommit.crema.global.auth.service.CustomOAuth2User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        // Principal 타입 안전성 검사
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomOAuth2User)) {
            log.error("Unexpected principal type: {}", principal != null ? principal.getClass().getSimpleName() : "null");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        CustomOAuth2User oAuth2User = (CustomOAuth2User) principal;
        String memberId = oAuth2User.getMember().getId();

        // 쿠키에 토큰 설정
        authService.setTokensToCookie(response, memberId);

        // 성공 페이지로 리다이렉트 (토큰 정보 없이)
        String targetUrl = redirectUri + "?status=success";

        response.sendRedirect(targetUrl);
    }
}