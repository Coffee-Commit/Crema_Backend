package coffeandcommit.crema.global.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.debug("인증되지 않은 요청입니다: {}", authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // JWT 에러 타입 결정
        String error = "invalid_token";
        String errorDescription = Optional.ofNullable(authException.getMessage())
                .orElse("Unauthorized");

        // 에러 메시지 기반으로 구체적인 에러 코드 결정
        if (errorDescription.contains("expired") || errorDescription.contains("만료")) {
            error = "expired_token";
        } else if (errorDescription.contains("invalid") || errorDescription.contains("유효하지")) {
            error = "invalid_token";
        }

        // RFC 6750 표준에 따른 WWW-Authenticate 헤더 설정
        String wwwAuthenticateValue = String.format(
                "Bearer error=\"%s\", error_description=\"%s\"",
                error,
                errorDescription.replace("\"", "\\\"") // 따옴표 이스케이프
        );
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, wwwAuthenticateValue);

        // JSON 응답 바디
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("error", "Unauthorized");
        responseMap.put("message", "인증이 필요합니다.");
        responseMap.put("status", HttpServletResponse.SC_UNAUTHORIZED);

        objectMapper.writeValue(response.getOutputStream(), responseMap);
    }
}