package coffeandcommit.crema.global.auth.handler;

import coffeandcommit.crema.global.auth.jwt.JwtTokenProvider;
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

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(
                oAuth2User.getMember().getUserId(),
                oAuth2User.getMember().getRole().name()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(oAuth2User.getMember().getUserId());

        // 쿠키에 토큰 설정
        authService.setTokensToCookie(response, accessToken, refreshToken);

        // 성공 페이지로 리다이렉트 (토큰 정보 없이)
        String targetUrl = redirectUri + "?status=success";

        log.info("OAuth2 login successful for user: {}, redirecting to: {}",
                oAuth2User.getMember().getUserId(), targetUrl);

        response.sendRedirect(targetUrl);
    }
}