package coffeandcommit.crema.global.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/debug")
@Profile({"local", "dev"}) // 프로덕션에서는 비활성화
@Tag(name = "Debug API", description = "개발환경 디버깅용 API")
public class DebugController {

    @Value("${app.oauth2.authorized-redirect-uri:NOT_SET}")
    private String redirectUri;

    @Value("${app.cookie.domain:NOT_SET}")
    private String cookieDomain;

    @Value("${app.cookie.same-site:NOT_SET}")
    private String cookieSameSite;

    @Value("${spring.profiles.active:NOT_SET}")
    private String activeProfile;

    @Value("${FRONTEND_URL:NOT_SET}")
    private String frontendUrl;

    @Value("${COOKIE_DOMAIN:NOT_SET}")
    private String envCookieDomain;

    @Value("${COOKIE_SAMESITE:NOT_SET}")
    private String envCookieSameSite;

    @Operation(summary = "환경설정 확인", description = "현재 서버의 주요 환경설정을 확인합니다.")
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new LinkedHashMap<>();

        // 핵심 설정들
        config.put("activeProfile", activeProfile);
        config.put("redirectUri", redirectUri);
        config.put("cookieDomain", cookieDomain);
        config.put("cookieSameSite", cookieSameSite);
        config.put("frontendUrl", frontendUrl);

        // 환경변수 원본값들
        Map<String, String> envVars = new LinkedHashMap<>();
        envVars.put("FRONTEND_URL", frontendUrl);
        envVars.put("COOKIE_DOMAIN", envCookieDomain);
        envVars.put("COOKIE_SAMESITE", envCookieSameSite);
        config.put("environmentVariables", envVars);

        // 서버 정보
        Map<String, String> serverInfo = new LinkedHashMap<>();
        serverInfo.put("javaVersion", System.getProperty("java.version"));
        serverInfo.put("osName", System.getProperty("os.name"));
        serverInfo.put("timestamp", String.valueOf(System.currentTimeMillis()));
        config.put("serverInfo", serverInfo);

        // 진단 정보
        Map<String, String> diagnosis = new LinkedHashMap<>();
        diagnosis.put("cookieDomainStatus", cookieDomain.equals("NOT_SET") ? "❌ 설정 안됨" : "✅ 설정됨");
        diagnosis.put("cookieSameSiteStatus", cookieSameSite.equals("NOT_SET") ? "❌ 설정 안됨" : "✅ 설정됨");
        diagnosis.put("frontendUrlStatus", frontendUrl.equals("NOT_SET") ? "❌ 설정 안됨" : "✅ 설정됨");
        config.put("diagnosis", diagnosis);

        log.info("Debug config requested: {}", config);
        return config;
    }

    @Operation(summary = "간단한 헬스체크", description = "서버가 살아있는지 확인")
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "OK");
        response.put("message", "Server is running");
        response.put("profile", activeProfile);
        response.put("timestamp", System.currentTimeMillis());

        log.info("Debug ping requested from profile: {}", activeProfile);
        return response;
    }
}