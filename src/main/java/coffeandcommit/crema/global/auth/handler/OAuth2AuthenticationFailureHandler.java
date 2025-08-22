package coffeandcommit.crema.global.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        // authorization_request_not_found 에러는 OAuth2 세션 상태가 일시적으로 손실된 경우로,
        // 실제 인증 실패가 아닌 브라우저 새로고침이나 네트워크 지연 등으로 발생하는 정상적인 상황이므로 로깅하지 않음
        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) exception;
            String errorCode = oauth2Exception.getError() != null ? oauth2Exception.getError().getErrorCode() : "unknown";

            if ("authorization_request_not_found".equals(errorCode)) {
                response.sendRedirect(redirectUri);
                return;
            }
        }

        // 실제 인증 실패인 경우에만 에러 로깅
        String errorDetails = getErrorDetails(exception);
        log.error("OAuth2 인증 실패: {}", errorDetails);
        log.error("Exception type: {}", exception.getClass().getSimpleName());
        log.error("Exception message: {}", exception.getMessage());

        if (exception.getCause() != null) {
            log.error("Exception cause: {}", exception.getCause().getMessage());
        }

        // URL 인코딩을 통해 특수문자 처리
        String encodedError = URLEncoder.encode(exception.getClass().getSimpleName(), StandardCharsets.UTF_8);
        String encodedMessage = exception.getLocalizedMessage() != null
                ? URLEncoder.encode(exception.getLocalizedMessage(), StandardCharsets.UTF_8)
                : URLEncoder.encode("Unknown error", StandardCharsets.UTF_8);

        String targetUrl = String.format("%s?status=error&error=%s&message=%s",
                redirectUri, encodedError, encodedMessage);

        log.info("Redirecting to error page: {}", targetUrl);
        response.sendRedirect(targetUrl);
    }

    private String getErrorDetails(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) exception;
            String errorCode = oauth2Exception.getError() != null ? oauth2Exception.getError().getErrorCode() : "unknown";
            String errorDescription = oauth2Exception.getError() != null ? oauth2Exception.getError().getDescription() : "No description";
            return String.format("OAuth2 Error - Code: %s, Description: %s", errorCode, errorDescription);
        }
        return exception.getMessage();
    }
}