package coffeandcommit.crema.global.auth.config;

import coffeandcommit.crema.global.auth.handler.OAuth2AuthenticationFailureHandler;
import coffeandcommit.crema.global.auth.handler.OAuth2AuthenticationSuccessHandler;
import coffeandcommit.crema.global.auth.jwt.JwtAuthenticationEntryPoint;
import coffeandcommit.crema.global.auth.jwt.JwtAuthenticationFilter;
import coffeandcommit.crema.global.auth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable()) // CSRF 완전 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable()) // 요청 캐시 비활성화 (JSESSIONID 제거)
                .securityContext(context -> context.requireExplicitSave(false)) // 보안 컨텍스트 자동 저장 비활성화
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // Prometheus Actuator Endpoint
                        .requestMatchers(EndpointRequest.to("prometheus")).permitAll()
                        // Public endpoints (인증 불필요)
                        .requestMatchers(
                                "/api/auth/status",
                                "/api/auth/refresh",
                                "/api/member/check/**",
                                "/api/test/auth/**",
                                "/api/debug/**", // 디버그 엔드포인트 추가
                                "/api/oauth2/**",
                                "/api/login/oauth2/**",
                                // Swagger UI
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                // Health check
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        // Auth endpoints (인증 필요)
                        .requestMatchers("/api/auth/**").authenticated()

                        // Member endpoints (인증 필요)
                        .requestMatchers("/api/member/**").authenticated()
                        .requestMatchers("/api/images/**").authenticated()

                        // 나머지 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                        .permitAll()
                );

        // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 개발 환경과 프로덕션 환경의 모든 도메인 포함
        configuration.setAllowedOriginPatterns(List.of(
                // 로컬 개발 환경
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:8080",
                // dev 서버 환경 (API 서버)
                "https://dev-api-coffeechat.kro.kr",
                // dev 서버 환경 (프론트엔드 서버 - 예상)
                "https://dev-coffeechat.kro.kr",
                "https://dev.coffeechat.kro.kr",
                // 프로덕션 환경
                "https://coffeechat.kro.kr",
                "https://api.coffeechat.kro.kr",
                // 모든 coffeechat.kro.kr 서브도메인 허용
                "https://*.coffeechat.kro.kr"
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // 쿠키 허용을 위해 필수

        // Preflight 요청 캐시 시간 설정 (1시간)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}